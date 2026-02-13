package com.weatherapp.controller;

import com.weatherapp.dto.WeatherDTO;
import com.weatherapp.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for data synchronization operations.
 *
 * Endpoints:
 * - POST /api/sync/{locationId} -> Sync weather data for a single location
 * - POST /api/sync/all          -> Sync weather data for all tracked locations
 */
@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sync", description = "Weather data synchronization and refresh")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/{locationId}")
    @Operation(summary = "Refresh weather data for a specific location on demand")
    public ResponseEntity<WeatherDTO.SyncResponse> syncLocation(@PathVariable String locationId) {
        return ResponseEntity.ok(syncService.syncLocation(locationId));
    }

    @PostMapping("/all")
    @Operation(summary = "Refresh weather data for all tracked locations")
    public ResponseEntity<List<WeatherDTO.SyncResponse>> syncAll() {
        return ResponseEntity.ok(syncService.syncAllLocations());
    }
}
