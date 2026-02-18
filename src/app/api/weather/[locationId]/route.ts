import { NextResponse } from "next/server";

// Enable dynamic API routes
export const dynamic = 'force-dynamic';

export async function GET(
  request: Request,
  { params }: { params: Promise<{ locationId: string }> }
) {
  try {
    // unwrap params first
    const { locationId } = await params;

    // Proxy to backend API
    const response = await fetch(
      `http://localhost:8080/api/weather/${locationId}`
    );

    const backendData = await response.json();

    // Transform backend response to frontend format
    const transformedData = {
      current: {
        temperature_2m: backendData.current?.temperature ?? 0,
        relative_humidity_2m: backendData.current?.humidity ?? 0,
        apparent_temperature: backendData.current?.apparentTemperature ?? 0,
        precipitation: backendData.current?.precipitation ?? 0,
        weather_code: backendData.current?.weatherCode ?? 0,
        wind_speed_10m: backendData.current?.windSpeed ?? 0,
      },
      daily: {
        time: backendData.dailyForecast?.map((item: any) => item.date) ?? [],
        temperature_2m_max: backendData.dailyForecast?.map((item: any) => item.temperatureMax) ?? [],
        temperature_2m_min: backendData.dailyForecast?.map((item: any) => item.temperatureMin) ?? [],
        weather_code: backendData.dailyForecast?.map((item: any) => item.weatherCode) ?? [],
      },
      hourly: {
        time: backendData.hourlyForecast?.map((item: any) => item.time) ?? [],
        temperature_2m: backendData.hourlyForecast?.map((item: any) => item.temperature) ?? [],
        weather_code: backendData.hourlyForecast?.map((item: any) => item.weatherCode) ?? [],
      },
      timezone: backendData.timezone || "UTC",
    };

    return NextResponse.json(transformedData);
  } catch (error) {
    console.error('Error transforming weather data:', error);
    return NextResponse.json(
      { error: "Failed to fetch weather data" },
      { status: 500 }
    );
  }
}
