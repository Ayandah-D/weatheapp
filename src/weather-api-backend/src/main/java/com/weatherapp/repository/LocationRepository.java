package com.weatherapp.repository;

import com.weatherapp.model.Location;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Location documents.
 * Provides CRUD operations and custom queries for tracked cities.
 */
@Repository
public interface LocationRepository extends MongoRepository<Location, String> {

    /** Find all locations marked as favorite, ordered by name */
    List<Location> findByFavoriteTrueOrderByNameAsc();

    /** Find all locations ordered by name */
    List<Location> findAllByOrderByNameAsc();

    /** Find a location by city name and country (case-insensitive) */
    Optional<Location> findByNameIgnoreCaseAndCountryIgnoreCase(String name, String country);

    /** Check if a location with the given name and country exists */
    boolean existsByNameIgnoreCaseAndCountryIgnoreCase(String name, String country);

    /** Find locations that have never been synced or are stale */
    List<Location> findBySyncStatusIn(List<Location.SyncStatus> statuses);

    /** Search locations by name (partial, case-insensitive) */
    List<Location> findByNameContainingIgnoreCase(String name);
}
