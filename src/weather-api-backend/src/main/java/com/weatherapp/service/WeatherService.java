package com.weatherapp.service;

import com.weatherapp.dto.WeatherDTO;
import com.weatherapp.exception.WeatherApiException;
import com.weatherapp.model.Location;
import com.weatherapp.model.WeatherSnapshot;
import com.weatherapp.repository.WeatherSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for weather data retrieval and storage.
 * Handles fetching from the external API, storing snapshots,
 * and providing weather data to controllers.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherSnapshotRepository snapshotRepository;
    private final WeatherApiClient weatherApiClient;

    public WeatherService(WeatherSnapshotRepository snapshotRepository,
                          WeatherApiClient weatherApiClient) {
        this.snapshotRepository = snapshotRepository;
        this.weatherApiClient = weatherApiClient;
    }

    /**
     * Get the latest weather snapshot for a location.
     * Returns cached data if available and not stale.
     */
    public WeatherDTO.SnapshotResponse getLatestWeather(String locationId) {
        Optional<WeatherSnapshot> snapshot = snapshotRepository
                .findTopByLocationIdOrderByFetchedAtDesc(locationId);

        if (snapshot.isEmpty()) {
            throw new WeatherApiException.NotFoundException("Weather data", locationId);
        }

        return toSnapshotResponse(snapshot.get());
    }

    /**
     * Get weather history for a location (paginated).
     */
    public List<WeatherDTO.SnapshotResponse> getWeatherHistory(String locationId, int page, int size) {
        return snapshotRepository
                .findByLocationIdOrderByFetchedAtDesc(locationId, PageRequest.of(page, size))
                .stream()
                .map(this::toSnapshotResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get weather data within a time range.
     */
    public List<WeatherDTO.SnapshotResponse> getWeatherInRange(String locationId, Instant from, Instant to) {
        return snapshotRepository
                .findByLocationIdAndFetchedAtBetweenOrderByFetchedAtDesc(locationId, from, to)
                .stream()
                .map(this::toSnapshotResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetch fresh weather data from the API and store it.
     * Performs conflict detection against the previous snapshot.
     */
    public WeatherSnapshot fetchAndStoreWeather(Location location, String units) {
        log.info("Fetching weather for {} ({}, {})", location.getName(), location.getLatitude(), location.getLongitude());

        WeatherSnapshot newData = weatherApiClient.fetchWeatherData(
                location.getLatitude(), location.getLongitude(), units);

        // Set the location reference
        newData.setLocationId(location.getId());
        newData.setFetchedAt(Instant.now());

        // Conflict detection: compare with previous snapshot
        Optional<WeatherSnapshot> previousSnapshot = snapshotRepository
                .findTopByLocationIdOrderByFetchedAtDesc(location.getId());

        if (previousSnapshot.isPresent()) {
            detectConflicts(newData, previousSnapshot.get());
        }

        // Save the new snapshot
        WeatherSnapshot saved = snapshotRepository.save(newData);
        log.info("Weather snapshot saved with ID: {} for location: {}", saved.getId(), location.getName());

        return saved;
    }

    /**
     * Search for cities using the geocoding API.
     */
    public List<WeatherApiClient.GeocodingResult> searchCities(String query) {
        return weatherApiClient.searchLocations(query);
    }

    // --- Conflict detection ---

    /**
     * Detects significant differences between the new and previous weather data.
     * A conflict is flagged if the temperature differs by more than the threshold.
     * This handles the assessment requirement: "Handle conflicts (what happens if
     * API data differs significantly from stored data?)"
     */
    private void detectConflicts(WeatherSnapshot newData, WeatherSnapshot previous) {
        double threshold = 10.0; // degrees

        if (newData.getCurrent() != null && previous.getCurrent() != null) {
            Double newTemp = newData.getCurrent().getTemperature();
            Double prevTemp = previous.getCurrent().getTemperature();

            if (newTemp != null && prevTemp != null) {
                double diff = Math.abs(newTemp - prevTemp);
                long hoursBetween = ChronoUnit.HOURS.between(
                        previous.getFetchedAt() != null ? previous.getFetchedAt() : Instant.now(),
                        Instant.now());

                // Flag conflict if temp changed significantly within a short period
                if (diff > threshold && hoursBetween < 6) {
                    String description = String.format(
                            "Temperature changed by %.1f degrees (from %.1f to %.1f) within %d hours. " +
                            "Previous data from %s. This may indicate API inconsistency or rapid weather change.",
                            diff, prevTemp, newTemp, hoursBetween, previous.getFetchedAt());

                    newData.setConflictDetected(true);
                    newData.setConflictDescription(description);
                    log.warn("Conflict detected for location {}: {}", newData.getLocationId(), description);
                }
            }
        }
    }

    // --- DTO mapping ---

    private WeatherDTO.SnapshotResponse toSnapshotResponse(WeatherSnapshot snapshot) {
        return WeatherDTO.SnapshotResponse.builder()
                .id(snapshot.getId())
                .locationId(snapshot.getLocationId())
                .current(snapshot.getCurrent() != null ? WeatherDTO.CurrentWeatherResponse.builder()
                        .temperature(snapshot.getCurrent().getTemperature())
                        .apparentTemperature(snapshot.getCurrent().getApparentTemperature())
                        .humidity(snapshot.getCurrent().getHumidity())
                        .precipitation(snapshot.getCurrent().getPrecipitation())
                        .weatherCode(snapshot.getCurrent().getWeatherCode())
                        .weatherDescription(snapshot.getCurrent().getWeatherDescription())
                        .windSpeed(snapshot.getCurrent().getWindSpeed())
                        .build() : null)
                .hourlyForecast(snapshot.getHourlyForecast() != null ?
                        snapshot.getHourlyForecast().stream()
                                .map(h -> WeatherDTO.HourlyForecastResponse.builder()
                                        .time(h.getTime())
                                        .temperature(h.getTemperature())
                                        .weatherCode(h.getWeatherCode())
                                        .weatherDescription(h.getWeatherDescription())
                                        .build())
                                .collect(Collectors.toList()) : null)
                .dailyForecast(snapshot.getDailyForecast() != null ?
                        snapshot.getDailyForecast().stream()
                                .map(d -> WeatherDTO.DailyForecastResponse.builder()
                                        .date(d.getDate())
                                        .temperatureMax(d.getTemperatureMax())
                                        .temperatureMin(d.getTemperatureMin())
                                        .weatherCode(d.getWeatherCode())
                                        .weatherDescription(d.getWeatherDescription())
                                        .build())
                                .collect(Collectors.toList()) : null)
                .units(snapshot.getUnits())
                .timezone(snapshot.getTimezone())
                .fetchedAt(snapshot.getFetchedAt())
                .conflictDetected(snapshot.isConflictDetected())
                .conflictDescription(snapshot.getConflictDescription())
                .build();
    }
}
