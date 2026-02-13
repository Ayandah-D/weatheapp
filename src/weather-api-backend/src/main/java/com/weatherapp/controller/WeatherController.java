package com.weatherapp.controller;

import com.weatherapp.dto.WeatherDTO;
import com.weatherapp.service.WeatherApiClient;
import com.weatherapp.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for weather data retrieval.
 *
 * Endpoints:
 * - GET /api/weather/{locationId}         -> Get latest weather for a location
 * - GET /api/weather/{locationId}/history -> Get historical weather snapshots
 * - GET /api/weather/search/cities        -> Search cities via geocoding API
 */
@RestController
@RequestMapping("/api/weather")
@Tag(name = "Weather", description = "Weather data retrieval and city search")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{locationId}")
    @Operation(summary = "Get the latest weather snapshot for a tracked location")
    public ResponseEntity<WeatherDTO.SnapshotResponse> getLatestWeather(
            @PathVariable String locationId) {
        return ResponseEntity.ok(weatherService.getLatestWeather(locationId));
    }

    @GetMapping("/{locationId}/history")
    @Operation(summary = "Get historical weather data for a location (paginated)")
    public ResponseEntity<List<WeatherDTO.SnapshotResponse>> getWeatherHistory(
            @PathVariable String locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(weatherService.getWeatherHistory(locationId, page, size));
    }

    @GetMapping("/search/cities")
    @Operation(summary = "Search for cities using the geocoding API")
    public ResponseEntity<List<WeatherApiClient.GeocodingResult>> searchCities(
            @RequestParam String query) {
        return ResponseEntity.ok(weatherService.searchCities(query));
    }
}
