"use client";

import { useState, useEffect, useCallback } from "react";
import {
  CloudSun,
  RefreshCw,
  MapPin,
  Clock,
  AlertTriangle,
  Loader2,
  Database,
  Server,
  Zap,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import WeatherCard from "@/components/weather-card";
import ForecastPanel from "@/components/forecast-panel";
import AddCityDialog from "@/components/add-city-dialog";
import SettingsPanel from "@/components/settings-panel";
import {
  fetchWeatherDirect,
  type GeocodingResult,
} from "@/lib/api";

// Backend API functions
async function fetchLocations(): Promise<TrackedLocation[]> {
  const response = await fetch('/api/locations');
  if (!response.ok) throw new Error('Failed to fetch locations');
  return response.json();
}

async function createLocation(city: GeocodingResult): Promise<TrackedLocation> {
  const response = await fetch('/api/locations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: city.name,
      country: city.country,
      latitude: city.latitude,
      longitude: city.longitude,
    }),
  });
  if (!response.ok) throw new Error('Failed to create location');
  return response.json();
}

async function fetchWeatherFromBackend(locationId: string): Promise<WeatherData> {
  const response = await fetch(`/api/weather/${locationId}`);
  if (!response.ok) throw new Error('Failed to fetch weather');
  return response.json();
}

async function syncLocation(locationId: string): Promise<void> {
  const response = await fetch(`/api/sync/${locationId}`, { method: 'POST' });
  if (!response.ok) throw new Error('Failed to sync location');
}

async function syncAllLocations(): Promise<void> {
  const response = await fetch('/api/sync/all', { method: 'POST' });
  if (!response.ok) throw new Error('Failed to sync all locations');
}

// Types for local state (mirrors Java DTOs)
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

// Default cities to seed the demo
const DEFAULT_CITIES: Array<{ name: string; country: string; lat: number; lon: number }> = [
  { name: "Victoria Falls", country: "Zimbabwe", lat: -17.9243, lon: 25.8572 },
  { name: "Cape Town", country: "South Africa", lat: -33.9249, lon: 18.4241 },
  { name: "Johannesburg", country: "South Africa", lat: -26.2041, lon: 28.0473 },
];

export default function WeatherDashboard() {
  const [locations, setLocations] = useState<TrackedLocation[]>([]);
  const [weatherMap, setWeatherMap] = useState<Record<string, WeatherData>>({});
  const [selectedLocation, setSelectedLocation] = useState<TrackedLocation | null>(null);
  const [units, setUnits] = useState("metric");
  const [refreshInterval, setRefreshInterval] = useState(0);
  const [loadingIds, setLoadingIds] = useState<Set<string>>(new Set());
  const [syncing, setSyncing] = useState(false);
  const [lastGlobalSync, setLastGlobalSync] = useState<Date | null>(null);
  const [initialized, setInitialized] = useState(false);

  // Initialize default cities
  useEffect(() => {
    if (initialized) return;
    
    // First, try to fetch existing locations from backend
    fetchLocations()
      .then((backendLocations) => {
        if (backendLocations.length > 0) {
          // Use backend locations and clear session storage to avoid ID conflicts
          setLocations(backendLocations);
          setSelectedLocation(backendLocations[0]);
          sessionStorage.removeItem("weather-locations"); // Clear conflicting IDs
          setInitialized(true);
        } else {
          // If no backend locations, check session storage
          const savedData = sessionStorage.getItem("weather-locations");
          if (savedData) {
            try {
              const parsed = JSON.parse(savedData);
              setLocations(parsed);
              if (parsed.length > 0) setSelectedLocation(parsed[0]);
              setInitialized(true);
              return;
            } catch { /* ignore */ }
          }

          // If no saved data, create default cities
          const initial: TrackedLocation[] = DEFAULT_CITIES.map((city, i) => ({
            id: `loc-${Date.now()}-${i}`,
            name: city.name,
            country: city.country,
            latitude: city.lat,
            longitude: city.lon,
            favorite: i === 0,
            lastSyncAt: null,
            syncStatus: "NEVER_SYNCED",
          }));
          setLocations(initial);
          setSelectedLocation(initial[0]);
          setInitialized(true);
        }
      })
      .catch((error) => {
        console.error('Failed to fetch locations from backend:', error);
        // Fallback to session storage or defaults
        const savedData = sessionStorage.getItem("weather-locations");
        if (savedData) {
          try {
            const parsed = JSON.parse(savedData);
            setLocations(parsed);
            if (parsed.length > 0) setSelectedLocation(parsed[0]);
            setInitialized(true);
            return;
          } catch { /* ignore */ }
        }

        const initial: TrackedLocation[] = DEFAULT_CITIES.map((city, i) => ({
          id: `loc-${Date.now()}-${i}`,
          name: city.name,
          country: city.country,
          latitude: city.lat,
          longitude: city.lon,
          favorite: i === 0,
          lastSyncAt: null,
          syncStatus: "NEVER_SYNCED",
        }));
        setLocations(initial);
        setSelectedLocation(initial[0]);
        setInitialized(true);
      });
  }, [initialized]);

  // Persist locations to session storage
  useEffect(() => {
    if (locations.length > 0) {
      sessionStorage.setItem("weather-locations", JSON.stringify(locations));
    }
  }, [locations]);

  // Fetch weather for a location
  const fetchWeather = useCallback(async (loc: TrackedLocation) => {
    setLoadingIds((prev) => new Set(prev).add(loc.id));
    setLocations((prev) =>
      prev.map((l) =>
        l.id === loc.id ? { ...l, syncStatus: "IN_PROGRESS" } : l
      )
    );

    try {
      // Try to fetch from backend first
      const data = await fetchWeatherFromBackend(loc.id);
      setWeatherMap((prev) => ({ ...prev, [loc.id]: data }));
      setLocations((prev) =>
        prev.map((l) =>
          l.id === loc.id
            ? { ...l, syncStatus: "SUCCESS", lastSyncAt: new Date().toISOString() }
            : l
        )
      );
    } catch (err) {
      console.error(`Failed to fetch weather for ${loc.name} from backend:`, err);
      // Fallback to direct API
      try {
        const data = await fetchWeatherDirect(loc.latitude, loc.longitude, units);
        setWeatherMap((prev) => ({ ...prev, [loc.id]: data }));
        setLocations((prev) =>
          prev.map((l) =>
            l.id === loc.id
              ? { ...l, syncStatus: "SUCCESS", lastSyncAt: new Date().toISOString() }
              : l
          )
        );
      } catch (directErr) {
        console.error(`Failed to fetch weather for ${loc.name} from direct API:`, directErr);
        setLocations((prev) =>
          prev.map((l) =>
            l.id === loc.id ? { ...l, syncStatus: "FAILED" } : l
          )
        );
      }
    } finally {
      setLoadingIds((prev) => {
        const next = new Set(prev);
        next.delete(loc.id);
        return next;
      });
    }
  }, [units]);

  // Auto-fetch on init and units change
  useEffect(() => {
    if (!initialized || locations.length === 0) return;
    locations.forEach((loc) => fetchWeather(loc));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialized, units]);

  // Auto-refresh interval
  useEffect(() => {
    if (refreshInterval <= 0) return;
    const interval = setInterval(() => {
      locations.forEach((loc) => fetchWeather(loc));
    }, refreshInterval * 60 * 1000);
    return () => clearInterval(interval);
  }, [refreshInterval, locations, fetchWeather]);

  // Sync all locations
  const handleSyncAll = async () => {
    setSyncing(true);
    for (const loc of locations) {
      await fetchWeather(loc);
    }
    setLastGlobalSync(new Date());
    setSyncing(false);
  };

  // Add a new city
  const handleAddCity = async (city: GeocodingResult) => {
    try {
      const newLoc = await createLocation(city);
      setLocations((prev) => [...prev, newLoc]);
      fetchWeather(newLoc);
    } catch (error) {
      console.error('Failed to add city:', error);
      // Fallback to local creation if backend fails
      const fallbackLoc: TrackedLocation = {
        id: `loc-${Date.now()}`,
        name: city.name,
        country: city.country,
        latitude: city.latitude,
        longitude: city.longitude,
        favorite: false,
        lastSyncAt: null,
        syncStatus: "NEVER_SYNCED",
      };
      setLocations((prev) => [...prev, fallbackLoc]);
      fetchWeather(fallbackLoc);
    }
  };

  // Delete a city
  const handleDelete = (id: string) => {
    setLocations((prev) => prev.filter((l) => l.id !== id));
    setWeatherMap((prev) => {
      const next = { ...prev };
      delete next[id];
      return next;
    });
    if (selectedLocation?.id === id) {
      setSelectedLocation(locations.find((l) => l.id !== id) || null);
    }
  };

  // Toggle favorite
  const handleToggleFavorite = (id: string, fav: boolean) => {
    setLocations((prev) =>
      prev.map((l) => (l.id === id ? { ...l, favorite: fav } : l))
    );
  };

  // Sort: favorites first, then alphabetically
  const sortedLocations = [...locations].sort((a, b) => {
    if (a.favorite && !b.favorite) return -1;
    if (!a.favorite && b.favorite) return 1;
    return a.name.localeCompare(b.name);
  });

  const syncedCount = locations.filter((l) => l.syncStatus === "SUCCESS").length;
  const failedCount = locations.filter((l) => l.syncStatus === "FAILED").length;

  return (
    <TooltipProvider>
      <div className="min-h-screen bg-background">
        {/* Header */}
        <header className="border-b border-border bg-card/50 backdrop-blur-sm sticky top-0 z-10">
          <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-primary/10">
                <CloudSun size={24} className="text-primary" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-foreground">
                  Weather Data Integration Platform
                </h1>
                <p className="text-xs text-muted-foreground">
                  Spring Boot + MongoDB + Open-Meteo API
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {/* Status Badges */}
              <div className="hidden md:flex items-center gap-2">
                <Tooltip>
                  <TooltipTrigger>
                    <Badge variant="outline" className="gap-1.5 py-1 border-border text-muted-foreground">
                      <MapPin size={12} />
                      {locations.length} cities
                    </Badge>
                  </TooltipTrigger>
                  <TooltipContent>Tracked locations</TooltipContent>
                </Tooltip>

                <Tooltip>
                  <TooltipTrigger>
                    <Badge variant="outline" className="gap-1.5 py-1 border-border text-muted-foreground">
                      <Database size={12} />
                      {syncedCount} synced
                    </Badge>
                  </TooltipTrigger>
                  <TooltipContent>Successfully synced locations</TooltipContent>
                </Tooltip>

                {failedCount > 0 && (
                  <Tooltip>
                    <TooltipTrigger>
                      <Badge variant="outline" className="gap-1.5 py-1 border-destructive text-destructive">
                        <AlertTriangle size={12} />
                        {failedCount} failed
                      </Badge>
                    </TooltipTrigger>
                    <TooltipContent>Failed syncs</TooltipContent>
                  </Tooltip>
                )}
              </div>

              {/* Sync All Button */}
              <Button
                variant="outline"
                size="sm"
                onClick={handleSyncAll}
                disabled={syncing || locations.length === 0}
                className="border-border gap-1.5"
              >
                {syncing ? (
                  <Loader2 size={14} className="animate-spin" />
                ) : (
                  <RefreshCw size={14} />
                )}
                Sync All
              </Button>

              <AddCityDialog
                onAdd={handleAddCity}
                existingCities={locations.map((l) => ({ name: l.name, country: l.country }))}
              />
              <SettingsPanel
                units={units}
                refreshInterval={refreshInterval}
                onUnitsChange={setUnits}
                onRefreshIntervalChange={setRefreshInterval}
              />
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="max-w-7xl mx-auto px-4 py-6">
          {/* Architecture Info Banner */}
          <Card className="bg-card mb-6 border-primary/20">
            <CardContent className="py-4 px-5">
              <div className="flex flex-wrap items-center gap-4 text-sm">
                <div className="flex items-center gap-2 text-primary">
                  <Server size={16} />
                  <span className="font-medium">Backend Architecture</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Badge className="bg-primary/10 text-primary border-0">Java 17 + Spring Boot 3</Badge>
                  <Badge className="bg-primary/10 text-primary border-0">MongoDB</Badge>
                  <Badge className="bg-primary/10 text-primary border-0">Open-Meteo API</Badge>
                  <Badge className="bg-primary/10 text-primary border-0">WebClient</Badge>
                  <Badge className="bg-primary/10 text-primary border-0">Caching</Badge>
                  <Badge className="bg-primary/10 text-primary border-0">Rate Limiting</Badge>
                </div>
                {lastGlobalSync && (
                  <div className="flex items-center gap-1.5 text-muted-foreground ml-auto">
                    <Clock size={14} />
                    <span>
                      Last sync: {lastGlobalSync.toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {locations.length === 0 ? (
            /* Empty State */
            <Card className="bg-card">
              <CardContent className="flex flex-col items-center justify-center py-20 text-center">
                <div className="p-4 rounded-full bg-primary/10 mb-4">
                  <MapPin size={40} className="text-primary/60" />
                </div>
                <h2 className="text-xl font-semibold text-foreground mb-2">
                  No Cities Tracked Yet
                </h2>
                <p className="text-muted-foreground mb-6 max-w-md">
                  Add cities to your watchlist to view current weather, forecasts, and sync data from the backend API.
                </p>
                <AddCityDialog
                  onAdd={handleAddCity}
                  existingCities={[]}
                />
              </CardContent>
            </Card>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left Column: City Cards */}
              <div className="lg:col-span-2 space-y-4">
                <div className="flex items-center justify-between mb-2">
                  <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                    <Zap size={18} className="text-primary" />
                    Tracked Locations
                  </h2>
                  <span className="text-xs text-muted-foreground">
                    {sortedLocations.filter((l) => l.favorite).length} favorites
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {sortedLocations.map((loc) => (
                    <WeatherCard
                      key={loc.id}
                      location={loc}
                      weather={weatherMap[loc.id] || null}
                      units={units}
                      loading={loadingIds.has(loc.id)}
                      onSync={() => fetchWeather(loc)}
                      onDelete={handleDelete}
                      onToggleFavorite={handleToggleFavorite}
                      onSelect={setSelectedLocation}
                      selected={selectedLocation?.id === loc.id}
                    />
                  ))}
                </div>
              </div>

              {/* Right Column: Forecast Detail */}
              <div className="space-y-4">
                {selectedLocation && weatherMap[selectedLocation.id] ? (
                  <ForecastPanel
                    locationName={selectedLocation.name}
                    weather={weatherMap[selectedLocation.id]}
                    units={units}
                  />
                ) : selectedLocation ? (
                  <Card className="bg-card">
                    <CardHeader>
                      <CardTitle className="text-lg text-foreground">
                        {selectedLocation.name}
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="text-center py-8">
                      <Loader2 size={32} className="animate-spin text-primary mx-auto mb-3" />
                      <p className="text-sm text-muted-foreground">
                        Loading forecast data...
                      </p>
                    </CardContent>
                  </Card>
                ) : (
                  <Card className="bg-card">
                    <CardContent className="text-center py-12 text-muted-foreground">
                      <CloudSun size={32} className="mx-auto mb-2 text-muted-foreground/40" />
                      <p className="text-sm">Select a city to view its forecast</p>
                    </CardContent>
                  </Card>
                )}

                {/* API Endpoints Reference */}
                <Card className="bg-card">
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm font-medium text-foreground flex items-center gap-2">
                      <Server size={14} className="text-primary" />
                      REST API Endpoints
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-1.5 text-xs font-mono text-muted-foreground">
                      <div className="flex gap-2">
                        <Badge className="bg-emerald-500/10 text-emerald-400 border-0 text-xs px-1.5">GET</Badge>
                        <span>/api/locations</span>
                      </div>
                      <div className="flex gap-2">
                        <Badge className="bg-sky-500/10 text-sky-400 border-0 text-xs px-1.5">POST</Badge>
                        <span>/api/locations</span>
                      </div>
                      <div className="flex gap-2">
                        <Badge className="bg-amber-500/10 text-amber-400 border-0 text-xs px-1.5">PUT</Badge>
                        <span>{"/api/locations/{id}"}</span>
                      </div>
                      <div className="flex gap-2">
                        <Badge className="bg-red-500/10 text-red-400 border-0 text-xs px-1.5">DEL</Badge>
                        <span>{"/api/locations/{id}"}</span>
                      </div>
                      <div className="flex gap-2">
                        <Badge className="bg-emerald-500/10 text-emerald-400 border-0 text-xs px-1.5">GET</Badge>
                        <span>{"/api/weather/{locationId}"}</span>
                      </div>
                      <div className="flex gap-2">
                        <Badge className="bg-sky-500/10 text-sky-400 border-0 text-xs px-1.5">POST</Badge>
                        <span>{"/api/sync/{locationId}"}</span>
                      </div>
                      <div className="flex gap-2">
                        <Badge className="bg-sky-500/10 text-sky-400 border-0 text-xs px-1.5">POST</Badge>
                        <span>/api/sync/all</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          )}
        </main>
      </div>
    </TooltipProvider>
  );
}
