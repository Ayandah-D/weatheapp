package com.weatherapp.service;

import com.weatherapp.dto.LocationDTO;
import com.weatherapp.exception.WeatherApiException;
import com.weatherapp.model.Location;
import com.weatherapp.repository.LocationRepository;
import com.weatherapp.repository.WeatherSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Location CRUD operations.
 * Handles creating, reading, updating, and deleting tracked cities.
 * Cascades delete operations to remove associated weather snapshots.
 */
@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository locationRepository;
    private final WeatherSnapshotRepository weatherSnapshotRepository;

    public LocationService(LocationRepository locationRepository,
                           WeatherSnapshotRepository weatherSnapshotRepository) {
        this.locationRepository = locationRepository;
        this.weatherSnapshotRepository = weatherSnapshotRepository;
    }

    /**
     * CREATE - Add a new city to track.
     * Validates that the city doesn't already exist in the database.
     */
    public LocationDTO.Response createLocation(LocationDTO.CreateRequest request) {
        log.info("Creating location: {}, {}", request.getName(), request.getCountry());

        // Check for duplicate
        if (locationRepository.existsByNameIgnoreCaseAndCountryIgnoreCase(
                request.getName(), request.getCountry())) {
            throw new WeatherApiException.DuplicateException(
                    "Location", request.getName() + ", " + request.getCountry());
        }

        Location location = Location.builder()
                .name(request.getName())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .displayName(request.getDisplayName())
                .favorite(request.isFavorite())
                .syncStatus(Location.SyncStatus.NEVER_SYNCED)
                .build();

        Location saved = locationRepository.save(location);
        log.info("Location created with ID: {}", saved.getId());

        return toResponse(saved);
    }

    /**
     * READ - Get all tracked locations.
     */
    public List<LocationDTO.Response> getAllLocations() {
        return locationRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * READ - Get a single location by ID.
     */
    public LocationDTO.Response getLocationById(String id) {
        Location location = findLocationOrThrow(id);
        return toResponse(location);
    }

    /**
     * READ - Get all favorite locations.
     */
    public List<LocationDTO.Response> getFavoriteLocations() {
        return locationRepository.findByFavoriteTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * READ - Search locations by name.
     */
    public List<LocationDTO.Response> searchLocations(String query) {
        return locationRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * UPDATE - Modify a tracked location (e.g., set as favorite, change display name).
     */
    public LocationDTO.Response updateLocation(String id, LocationDTO.UpdateRequest request) {
        Location location = findLocationOrThrow(id);

        if (request.getDisplayName() != null) {
            location.setDisplayName(request.getDisplayName());
        }
        if (request.getFavorite() != null) {
            location.setFavorite(request.getFavorite());
        }

        Location updated = locationRepository.save(location);
        log.info("Location updated: {}", updated.getId());

        return toResponse(updated);
    }

    /**
     * DELETE - Remove a city from tracking.
     * Also deletes all associated weather snapshots (cascade).
     */
    @Transactional
    public void deleteLocation(String id) {
        Location location = findLocationOrThrow(id);

        // Cascade delete: remove all weather snapshots for this location
        long deletedSnapshots = weatherSnapshotRepository.countByLocationId(id);
        weatherSnapshotRepository.deleteByLocationId(id);
        log.info("Deleted {} weather snapshots for location {}", deletedSnapshots, id);

        locationRepository.delete(location);
        log.info("Location deleted: {} ({})", location.getName(), id);
    }

    // --- Internal helpers ---

    /** Find a location by ID or throw NotFoundException */
    public Location findLocationOrThrow(String id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new WeatherApiException.NotFoundException("Location", id));
    }

    /** Convert entity to response DTO */
    private LocationDTO.Response toResponse(Location location) {
        return LocationDTO.Response.builder()
                .id(location.getId())
                .name(location.getName())
                .country(location.getCountry())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .displayName(location.getDisplayName())
                .favorite(location.isFavorite())
                .lastSyncAt(location.getLastSyncAt())
                .syncStatus(location.getSyncStatus().name())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
