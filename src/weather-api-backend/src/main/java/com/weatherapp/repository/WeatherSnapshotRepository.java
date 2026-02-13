package com.weatherapp.repository;

import com.weatherapp.model.WeatherSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WeatherSnapshot documents.
 * Provides queries for weather data retrieval and history.
 */
@Repository
public interface WeatherSnapshotRepository extends MongoRepository<WeatherSnapshot, String> {

    /** Get the most recent weather snapshot for a location */
    Optional<WeatherSnapshot> findTopByLocationIdOrderByFetchedAtDesc(String locationId);

    /** Get weather history for a location, newest first */
    List<WeatherSnapshot> findByLocationIdOrderByFetchedAtDesc(String locationId, Pageable pageable);

    /** Get all snapshots for a location within a time range */
    List<WeatherSnapshot> findByLocationIdAndFetchedAtBetweenOrderByFetchedAtDesc(
            String locationId, Instant from, Instant to);

    /** Delete all snapshots for a location (cascade on location delete) */
    void deleteByLocationId(String locationId);

    /** Count snapshots for a given location */
    long countByLocationId(String locationId);

    /** Find snapshots where conflicts were detected */
    List<WeatherSnapshot> findByConflictDetectedTrue();
}
