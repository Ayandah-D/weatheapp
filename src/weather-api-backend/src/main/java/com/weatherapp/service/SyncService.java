package com.weatherapp.service;

import com.weatherapp.dto.WeatherDTO;
import com.weatherapp.model.Location;
import com.weatherapp.model.WeatherSnapshot;
import com.weatherapp.repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for synchronizing weather data across all tracked locations.
 *
 * Implements:
 * - On-demand refresh for single locations or all locations
 * - Per-location sync timestamps
 * - Automatic periodic sync via @Scheduled
 * - Conflict detection when API data differs significantly from stored data
 * - Stale data detection
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final LocationRepository locationRepository;
    private final WeatherService weatherService;
    private final PreferenceService preferenceService;

    @Value("${weather.sync.stale-threshold-minutes:60}")
    private int staleThresholdMinutes;

    public SyncService(LocationRepository locationRepository,
                       WeatherService weatherService,
                       PreferenceService preferenceService) {
        this.locationRepository = locationRepository;
        this.weatherService = weatherService;
        this.preferenceService = preferenceService;
    }

    /**
     * Sync weather data for a single location on demand.
     *
     * @param locationId The location to sync
     * @return SyncResponse with status and details
     */
    public WeatherDTO.SyncResponse syncLocation(@NonNull String locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new com.weatherapp.exception.WeatherApiException.NotFoundException("Location", locationId));

        return performSync(location);
    }

    /**
     * Sync weather data for all tracked locations on demand.
     *
     * @return List of SyncResponses for each location
     */
    public List<WeatherDTO.SyncResponse> syncAllLocations() {
        List<Location> locations = locationRepository.findAll();
        List<WeatherDTO.SyncResponse> results = new ArrayList<>();

        log.info("Starting sync for {} locations", locations.size());

        for (Location location : locations) {
            results.add(performSync(location));
        }

        long successCount = results.stream().filter(WeatherDTO.SyncResponse::isSuccess).count();
        log.info("Sync complete: {}/{} successful", successCount, results.size());

        return results;
    }

    /**
     * Automatic periodic sync triggered by Spring scheduler.
     * Runs every 30 minutes by default.
     * Only syncs locations that are stale or have never been synced.
     */
    @Scheduled(fixedDelayString = "${weather.sync.default-interval-minutes:30}000" + "0")
    public void scheduledSync() {
        log.info("Starting scheduled sync...");

        List<Location> staleLocations = locationRepository.findAll().stream()
                .filter(this::isStale)
                .toList();

        if (staleLocations.isEmpty()) {
            log.info("No stale locations found. Skipping sync.");
            return;
        }

        log.info("Found {} stale locations to sync", staleLocations.size());
        for (Location location : staleLocations) {
            performSync(location);
        }
    }

    /**
     * Check if a location's data is stale (older than the configured threshold).
     */
    public boolean isStale(Location location) {
        if (location.getLastSyncAt() == null) return true;
        return location.getLastSyncAt().isBefore(
                Instant.now().minus(staleThresholdMinutes, ChronoUnit.MINUTES));
    }

    // --- Private sync logic ---

    private WeatherDTO.SyncResponse performSync(Location location) {
        String units = preferenceService.getEffectiveUnits();

        // Mark as in-progress
        location.setSyncStatus(Location.SyncStatus.IN_PROGRESS);
        locationRepository.save(location);

        try {
            WeatherSnapshot snapshot = weatherService.fetchAndStoreWeather(location, units);

            // Update location sync metadata
            location.setLastSyncAt(Instant.now());
            location.setSyncStatus(Location.SyncStatus.SUCCESS);
            locationRepository.save(location);

            log.info("Successfully synced weather for: {}", location.getName());

            return WeatherDTO.SyncResponse.builder()
                    .locationId(location.getId())
                    .locationName(location.getName())
                    .success(true)
                    .message("Weather data synced successfully")
                    .syncedAt(Instant.now())
                    .conflictDetected(snapshot.isConflictDetected())
                    .conflictDescription(snapshot.getConflictDescription())
                    .build();

        } catch (Exception e) {
            log.error("Failed to sync weather for {}: {}", location.getName(), e.getMessage());

            // Mark as failed
            location.setSyncStatus(Location.SyncStatus.FAILED);
            locationRepository.save(location);

            return WeatherDTO.SyncResponse.builder()
                    .locationId(location.getId())
                    .locationName(location.getName())
                    .success(false)
                    .message("Sync failed: " + e.getMessage())
                    .syncedAt(Instant.now())
                    .conflictDetected(false)
                    .build();
        }
    }
}
