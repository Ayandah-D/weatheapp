/**
 * API client for the Spring Boot Weather Backend.
 * All calls go through the Next.js API routes which proxy to the Java backend.
 * For direct connection, set NEXT_PUBLIC_API_URL to the backend URL.
 */

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "https://api.open-meteo.com/v1";
const GEOCODING_BASE = "https://geocoding-api.open-meteo.com/v1";

// --- Weather code descriptions (WMO codes) ---
const WEATHER_DESCRIPTIONS: Record<number, string> = {
  0: "Clear sky",
  1: "Mainly clear",
  2: "Partly cloudy",
  3: "Overcast",
  45: "Fog",
  48: "Depositing rime fog",
  51: "Light drizzle",
  53: "Moderate drizzle",
  55: "Dense drizzle",
  61: "Slight rain",
  63: "Moderate rain",
  65: "Heavy rain",
  71: "Slight snow",
  73: "Moderate snow",
  75: "Heavy snow",
  80: "Rain showers",
  81: "Moderate rain showers",
  82: "Violent rain showers",
  85: "Snow showers",
  95: "Thunderstorm",
  96: "Thunderstorm with hail",
  99: "Thunderstorm with heavy hail",
};

export function getWeatherDescription(code: number): string {
  return WEATHER_DESCRIPTIONS[code] || "Unknown";
}

export function getWeatherIcon(code: number): string {
  if (code === 0) return "sun";
  if (code <= 3) return "cloud-sun";
  if (code <= 48) return "cloud-fog";
  if (code <= 57) return "cloud-drizzle";
  if (code <= 67) return "cloud-rain";
  if (code <= 77) return "snowflake";
  if (code <= 82) return "cloud-rain-wind";
  if (code <= 86) return "snowflake";
  return "cloud-lightning";
}

// --- Types matching Java DTOs ---

export interface LocationResponse {
  id: string;
  name: string;
  country: string;
  latitude: number;
  longitude: number;
  displayName: string | null;
  favorite: boolean;
  lastSyncAt: string | null;
  syncStatus: string;
  createdAt: string;
  updatedAt: string;
}

export interface WeatherSnapshotResponse {
  id: string;
  locationId: string;
  current: {
    temperature: number;
    apparentTemperature: number;
    humidity: number;
    precipitation: number;
    weatherCode: number;
    weatherDescription: string;
    windSpeed: number;
  };
  hourlyForecast: Array<{
    time: string;
    temperature: number;
    weatherCode: number;
    weatherDescription: string;
  }>;
  dailyForecast: Array<{
    date: string;
    temperatureMax: number;
    temperatureMin: number;
    weatherCode: number;
    weatherDescription: string;
  }>;
  units: string;
  timezone: string;
  fetchedAt: string;
  conflictDetected: boolean;
  conflictDescription: string | null;
}

export interface SyncResponse {
  locationId: string;
  locationName: string;
  success: boolean;
  message: string;
  syncedAt: string;
  conflictDetected: boolean;
  conflictDescription: string | null;
}

export interface GeocodingResult {
  name: string;
  country: string;
  countryCode: string;
  latitude: number;
  longitude: number;
  admin1: string;
}

export interface PreferenceResponse {
  id: string;
  userId: string;
  units: string;
  refreshIntervalMinutes: number;
  windSpeedUnit: string;
  precipitationUnit: string;
  defaultLocationId: string | null;
  theme: string;
}

// --- Direct Open-Meteo API calls (frontend-only mode) ---

export async function fetchWeatherDirect(lat: number, lon: number, units: string = "metric") {
  const tempUnit = units === "metric" ? "celsius" : "fahrenheit";
  const windUnit = units === "metric" ? "kmh" : "mph";
  const precipUnit = units === "metric" ? "mm" : "inch";

  const url = `${API_BASE}/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&hourly=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&temperature_unit=${tempUnit}&wind_speed_unit=${windUnit}&precipitation_unit=${precipUnit}&timezone=auto`;

  const res = await fetch(url);
  if (!res.ok) throw new Error(`Weather API error: ${res.status}`);
  return res.json();
}

export async function searchCitiesDirect(query: string): Promise<GeocodingResult[]> {
  const url = `${GEOCODING_BASE}/search?name=${encodeURIComponent(query)}&count=8&language=en&format=json`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Geocoding API error: ${res.status}`);
  const data = await res.json();
  return (data.results || []).map((r: Record<string, unknown>) => ({
    name: r.name as string,
    country: r.country as string,
    countryCode: r.country_code as string,
    latitude: r.latitude as number,
    longitude: r.longitude as number,
    admin1: (r.admin1 as string) || "",
  }));
}

// --- SWR fetcher ---
export const fetcher = (url: string) => fetch(url).then((r) => r.json());
