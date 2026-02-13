"use client";

import { useState } from "react";
import { MapPin, Search, Loader2, Plus } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { searchCitiesDirect, type GeocodingResult } from "@/lib/api";

interface AddCityDialogProps {
  onAdd: (city: GeocodingResult) => void;
  existingCities: Array<{ name: string; country: string }>;
}

export default function AddCityDialog({ onAdd, existingCities }: AddCityDialogProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<GeocodingResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setError("");
    try {
      const data = await searchCitiesDirect(query);
      setResults(data);
      if (data.length === 0) setError("No cities found. Try a different search term.");
    } catch {
      setError("Failed to search cities. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = (city: GeocodingResult) => {
    onAdd(city);
    setOpen(false);
    setQuery("");
    setResults([]);
  };

  const isAlreadyTracked = (city: GeocodingResult) =>
    existingCities.some(
      (ec) =>
        ec.name.toLowerCase() === city.name.toLowerCase() &&
        ec.country.toLowerCase() === city.country.toLowerCase()
    );

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button className="bg-primary text-primary-foreground hover:bg-primary/90">
          <Plus size={18} className="mr-2" />
          Add City
        </Button>
      </DialogTrigger>
      <DialogContent className="bg-card border-border sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-foreground">Add a City to Track</DialogTitle>
        </DialogHeader>

        <div className="flex gap-2 mt-2">
          <Input
            placeholder="Search for a city..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            className="bg-secondary border-border text-foreground placeholder:text-muted-foreground"
          />
          <Button
            onClick={handleSearch}
            disabled={loading || !query.trim()}
            className="bg-primary text-primary-foreground"
          >
            {loading ? (
              <Loader2 size={18} className="animate-spin" />
            ) : (
              <Search size={18} />
            )}
          </Button>
        </div>

        {error && (
          <p className="text-sm text-destructive mt-2">{error}</p>
        )}

        {results.length > 0 && (
          <div className="mt-3 max-h-64 overflow-y-auto space-y-1.5">
            {results.map((city, i) => {
              const tracked = isAlreadyTracked(city);
              return (
                <button
                  key={`${city.name}-${city.country}-${i}`}
                  onClick={() => !tracked && handleAdd(city)}
                  disabled={tracked}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-left transition-colors ${
                    tracked
                      ? "bg-secondary/30 text-muted-foreground cursor-not-allowed"
                      : "bg-secondary/50 hover:bg-secondary text-foreground"
                  }`}
                >
                  <MapPin size={16} className="text-primary/60 shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-sm truncate">
                      {city.name}
                      {city.admin1 ? `, ${city.admin1}` : ""}
                    </div>
                    <div className="text-xs text-muted-foreground">{city.country}</div>
                  </div>
                  <div className="text-xs text-muted-foreground shrink-0 font-mono">
                    {city.latitude.toFixed(2)}, {city.longitude.toFixed(2)}
                  </div>
                  {tracked && (
                    <span className="text-xs bg-primary/20 text-primary px-2 py-0.5 rounded">
                      Tracked
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
