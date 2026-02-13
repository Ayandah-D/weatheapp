package com.weatherapp.controller;

import com.weatherapp.dto.LocationDTO;
import com.weatherapp.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Location CRUD operations.
 *
 * Endpoints:
 * - POST   /api/locations         -> Create a new tracked location
 * - GET    /api/locations         -> List all tracked locations
 * - GET    /api/locations/{id}    -> Get a specific location
 * - PUT    /api/locations/{id}    -> Update a location (favorite, display name)
 * - DELETE /api/locations/{id}    -> Remove a location (cascades to weather data)
 * - GET    /api/locations/favorites -> List favorite locations
 * - GET    /api/locations/search  -> Search tracked locations by name
 */
@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "CRUD operations for tracked weather locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    @Operation(summary = "Add a new city to track")
    public ResponseEntity<LocationDTO.Response> createLocation(
            @Valid @RequestBody LocationDTO.CreateRequest request) {
        LocationDTO.Response response = locationService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all tracked cities")
    public ResponseEntity<List<LocationDTO.Response>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific tracked city by ID")
    public ResponseEntity<LocationDTO.Response> getLocationById(@PathVariable String id) {
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a tracked location (favorite, display name)")
    public ResponseEntity<LocationDTO.Response> updateLocation(
            @PathVariable String id,
            @RequestBody LocationDTO.UpdateRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a city from tracking (cascades to weather data)")
    public ResponseEntity<Void> deleteLocation(@PathVariable String id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get all favorite locations")
    public ResponseEntity<List<LocationDTO.Response>> getFavoriteLocations() {
        return ResponseEntity.ok(locationService.getFavoriteLocations());
    }

    @GetMapping("/search")
    @Operation(summary = "Search tracked locations by name")
    public ResponseEntity<List<LocationDTO.Response>> searchLocations(
            @RequestParam String query) {
        return ResponseEntity.ok(locationService.searchLocations(query));
    }
}
