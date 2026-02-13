"use client";

import {
  Cloud,
  CloudDrizzle,
  CloudFog,
  CloudLightning,
  CloudRain,
  CloudRainWind,
  CloudSun,
  Snowflake,
  Sun,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { getWeatherDescription } from "@/lib/api";

interface WeatherData {
  current: {
    temperature_2m: number;
    relative_humidity_2m: number;
    apparent_temperature: number;
    precipitation: number;
    weather_code: number;
    wind_speed_10m: number;
  };
  daily: {
    time: string[];
    temperature_2m_max: number[];
    temperature_2m_min: number[];
    weather_code: number[];
  };
  hourly: {
    time: string[];
    temperature_2m: number[];
    weather_code: number[];
  };
  timezone: string;
}

function getSmallIcon(code: number) {
  const props = { size: 18, className: "text-primary/80" };
  if (code === 0) return <Sun {...props} />;
  if (code <= 2) return <CloudSun {...props} />;
  if (code <= 3) return <Cloud {...props} />;
  if (code <= 48) return <CloudFog {...props} />;
  if (code <= 57) return <CloudDrizzle {...props} />;
  if (code <= 67) return <CloudRain {...props} />;
  if (code <= 77) return <Snowflake {...props} />;
  if (code <= 82) return <CloudRainWind {...props} />;
  if (code <= 86) return <Snowflake {...props} />;
  return <CloudLightning {...props} />;
}

function getDayName(dateString: string) {
  const date = new Date(dateString);
  const today = new Date();
  if (date.toDateString() === today.toDateString()) return "Today";
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  if (date.toDateString() === tomorrow.toDateString()) return "Tomorrow";
  return date.toLocaleDateString("en-US", { weekday: "short" });
}

function getHourLabel(timeString: string) {
  const date = new Date(timeString);
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

interface ForecastPanelProps {
  locationName: string;
  weather: WeatherData;
  units: string;
}

export default function ForecastPanel({
  locationName,
  weather,
  units,
}: ForecastPanelProps) {
  const tempUnit = units === "metric" ? "C" : "F";

  // Get next 24 hours of hourly data
  const now = new Date();
  const hourlySlice = weather.hourly.time
    .map((t, i) => ({ time: t, temp: weather.hourly.temperature_2m[i], code: weather.hourly.weather_code[i] }))
    .filter((h) => new Date(h.time) >= now)
    .slice(0, 24);

  return (
    <Card className="bg-card">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg text-foreground">
          Forecast for {locationName}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <Tabs defaultValue="daily" className="w-full">
          <TabsList className="grid w-full grid-cols-2 bg-secondary">
            <TabsTrigger value="daily">5-Day Forecast</TabsTrigger>
            <TabsTrigger value="hourly">Hourly</TabsTrigger>
          </TabsList>

          <TabsContent value="daily" className="mt-4">
            <div className="space-y-2">
              {weather.daily.time.map((date, i) => (
                <div
                  key={date}
                  className="flex items-center justify-between px-3 py-2.5 rounded-lg bg-secondary/50 hover:bg-secondary transition-colors"
                >
                  <div className="flex items-center gap-3 w-28">
                    {getSmallIcon(weather.daily.weather_code[i])}
                    <span className="text-sm font-medium text-foreground">
                      {getDayName(date)}
                    </span>
                  </div>
                  <span className="text-xs text-muted-foreground flex-1 text-center">
                    {getWeatherDescription(weather.daily.weather_code[i])}
                  </span>
                  <div className="flex items-center gap-2 text-sm">
                    <span className="text-foreground font-medium">
                      {Math.round(weather.daily.temperature_2m_max[i])}{"°"}
                    </span>
                    <span className="text-muted-foreground">
                      {Math.round(weather.daily.temperature_2m_min[i])}{"°"}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </TabsContent>

          <TabsContent value="hourly" className="mt-4">
            <div className="space-y-1.5 max-h-80 overflow-y-auto pr-1">
              {hourlySlice.map((h) => (
                <div
                  key={h.time}
                  className="flex items-center justify-between px-3 py-2 rounded-lg bg-secondary/50 hover:bg-secondary transition-colors"
                >
                  <div className="flex items-center gap-3 w-20">
                    {getSmallIcon(h.code)}
                    <span className="text-sm text-muted-foreground">
                      {getHourLabel(h.time)}
                    </span>
                  </div>
                  <span className="text-xs text-muted-foreground flex-1 text-center">
                    {getWeatherDescription(h.code)}
                  </span>
                  <span className="text-sm font-medium text-foreground">
                    {Math.round(h.temp)}{"°"}{tempUnit}
                  </span>
                </div>
              ))}
            </div>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
