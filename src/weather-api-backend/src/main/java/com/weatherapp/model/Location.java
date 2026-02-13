package com.weatherapp.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents a tracked city/location in the weather system.
 * Stores the city name, country, coordinates, and tracking metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "locations")
@CompoundIndex(name = "city_country_idx", def = "{'name': 1, 'country': 1}", unique = true)
public class Location {

    @Id
    private String id;

    @NotBlank(message = "City name is required")
    private String name;

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    /** Optional display name set by the user */
    private String displayName;

    /** Whether this location is marked as a favorite */
    @Builder.Default
    private boolean favorite = false;

    /** Timestamp of the last successful weather data sync */
    private Instant lastSyncAt;

    /** Status of the last sync attempt */
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.NEVER_SYNCED;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum SyncStatus {
        NEVER_SYNCED,
        SUCCESS,
        FAILED,
        IN_PROGRESS,
        STALE
    }
}
