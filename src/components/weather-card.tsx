"use client";

import {
  Cloud,
  CloudDrizzle,
  CloudFog,
  CloudLightning,
  CloudRain,
  CloudRainWind,
  CloudSun,
  Droplets,
  Heart,
  RefreshCw,
  Snowflake,
  Sun,
  Thermometer,
  Trash2,
  Wind,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
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

interface TrackedLocation {
  id: string;
  name: string;
  country: string;
  latitude: number;
  longitude: number;
  favorite: boolean;
  lastSyncAt: string | null;
  syncStatus: string;
}

interface WeatherCardProps {
  location: TrackedLocation;
  weather: WeatherData | null;
  units: string;
  loading?: boolean;
  onSync: (id: string) => Promise<void>;
  onDelete: (id: string) => void;
  onToggleFavorite: (id: string, fav: boolean) => void;
  onSelect: (location: TrackedLocation) => void;
  selected?: boolean;
}

function getIcon(code: number, size: number = 24) {
  const props = { size, className: "text-primary" };
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

export default function WeatherCard({
  location,
  weather,
  units,
  loading,
  onSync,
  onDelete,
  onToggleFavorite,
  onSelect,
  selected,
}: WeatherCardProps) {
  const tempUnit = units === "metric" ? "C" : "F";
  const windUnit = units === "metric" ? "km/h" : "mph";

  return (
    <Card
      className={`cursor-pointer transition-all duration-200 hover:border-primary/40 ${
        selected ? "border-primary ring-1 ring-primary/30" : ""
      } bg-card`}
      onClick={() => onSelect(location)}
    >
      <CardContent className="p-5">
        {/* Header */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-foreground truncate text-lg">
              {location.name}
            </h3>
            <p className="text-sm text-muted-foreground">{location.country}</p>
          </div>
          <div className="flex items-center gap-1 ml-2">
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={(e) => {
                e.stopPropagation();
                onToggleFavorite(location.id, !location.favorite);
              }}
              aria-label={location.favorite ? "Remove from favorites" : "Add to favorites"}
            >
              <Heart
                size={16}
                className={
                  location.favorite
                    ? "fill-red-500 text-red-500"
                    : "text-muted-foreground"
                }
              />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={(e) => {
                e.stopPropagation();
                onSync(location.id);
              }}
              disabled={loading}
              aria-label="Sync weather data"
            >
              <RefreshCw
                size={16}
                className={`text-muted-foreground ${loading ? "animate-spin" : ""}`}
              />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-muted-foreground hover:text-destructive"
              onClick={(e) => {
                e.stopPropagation();
                onDelete(location.id);
              }}
              aria-label="Remove location"
            >
              <Trash2 size={16} />
            </Button>
          </div>
        </div>

        {/* Weather Data */}
        {weather ? (
          <>
            <div className="flex items-center gap-4 mb-4">
              {getIcon(weather.current?.weather_code ?? 0, 40)}
              <div>
                <div className="text-3xl font-bold text-foreground">
                  {Math.round(weather.current?.temperature_2m ?? 0)}
                  {"°"}
                  {tempUnit}
                </div>
                <p className="text-sm text-muted-foreground">
                  {getWeatherDescription(weather.current?.weather_code ?? 0)}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-3 text-sm">
              <div className="flex items-center gap-1.5 text-muted-foreground">
                <Thermometer size={14} className="text-primary/70" />
                <span>
                  {"Feels "}
                  {Math.round(weather.current?.apparent_temperature ?? 0)}
                  {"°"}
                </span>
              </div>
              <div className="flex items-center gap-1.5 text-muted-foreground">
                <Droplets size={14} className="text-primary/70" />
                <span>{weather.current?.relative_humidity_2m ?? 0}%</span>
              </div>
              <div className="flex items-center gap-1.5 text-muted-foreground">
                <Wind size={14} className="text-primary/70" />
                <span>
                  {Math.round(weather.current?.wind_speed_10m ?? 0)} {windUnit}
                </span>
              </div>
            </div>

            {/* Sync status */}
            <div className="mt-4 pt-3 border-t border-border">
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <span
                    className={`inline-block w-1.5 h-1.5 rounded-full ${
                      location.syncStatus === "SUCCESS"
                        ? "bg-emerald-500"
                        : location.syncStatus === "FAILED"
                        ? "bg-red-500"
                        : location.syncStatus === "IN_PROGRESS"
                        ? "bg-amber-500"
                        : "bg-muted-foreground"
                    }`}
                  />
                  {location.syncStatus === "NEVER_SYNCED"
                    ? "Never synced"
                    : location.syncStatus}
                </span>
                {location.lastSyncAt && (
                  <span>
                    {new Date(location.lastSyncAt).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                )}
              </div>
            </div>
          </>
        ) : (
          <div className="text-center py-6 text-muted-foreground text-sm">
            <Cloud size={32} className="mx-auto mb-2 text-muted-foreground/40" />
            <p>No weather data yet</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-2"
              onClick={(e) => {
                e.stopPropagation();
                onSync(location.id);
              }}
            >
              <RefreshCw size={14} className="mr-1" />
              Fetch Weather
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
