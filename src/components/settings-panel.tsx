"use client";

import { Settings, Thermometer } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface SettingsPanelProps {
  units: string;
  refreshInterval: number;
  onUnitsChange: (units: string) => void;
  onRefreshIntervalChange: (minutes: number) => void;
}

export default function SettingsPanel({
  units,
  refreshInterval,
  onUnitsChange,
  onRefreshIntervalChange,
}: SettingsPanelProps) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" size="icon" className="border-border" aria-label="Settings">
          <Settings size={18} />
        </Button>
      </DialogTrigger>
      <DialogContent className="bg-card border-border sm:max-w-sm">
        <DialogHeader>
          <DialogTitle className="text-foreground flex items-center gap-2">
            <Settings size={20} />
            Preferences
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6 mt-4">
          {/* Units Toggle */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Thermometer size={16} className="text-primary" />
              <Label className="text-foreground">Imperial Units (F)</Label>
            </div>
            <Switch
              checked={units === "imperial"}
              onCheckedChange={(checked) =>
                onUnitsChange(checked ? "imperial" : "metric")
              }
            />
          </div>

          {/* Refresh Interval */}
          <div className="space-y-2">
            <Label className="text-foreground">Auto Refresh Interval</Label>
            <Select
              value={String(refreshInterval)}
              onValueChange={(val) => onRefreshIntervalChange(Number(val))}
            >
              <SelectTrigger className="bg-secondary border-border text-foreground">
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="bg-card border-border">
                <SelectItem value="0">Disabled</SelectItem>
                <SelectItem value="15">Every 15 minutes</SelectItem>
                <SelectItem value="30">Every 30 minutes</SelectItem>
                <SelectItem value="60">Every hour</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Info Section */}
          <div className="rounded-lg bg-secondary/50 p-3 text-xs text-muted-foreground space-y-1">
            <p className="font-medium text-foreground/80">Backend API Endpoints:</p>
            <p className="font-mono">GET  /api/locations</p>
            <p className="font-mono">POST /api/locations</p>
            <p className="font-mono">PUT  /api/locations/:id</p>
            <p className="font-mono">DELETE /api/locations/:id</p>
            <p className="font-mono">GET  /api/weather/:locationId</p>
            <p className="font-mono">POST /api/sync/:locationId</p>
            <p className="font-mono">POST /api/sync/all</p>
            <p className="font-mono">GET  /api/preferences</p>
            <p className="font-mono">PUT  /api/preferences</p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
