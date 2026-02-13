package com.weatherapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Stores user preferences for the weather application.
 * Uses a userId field to associate preferences with a user session.
 * For this assessment, a default "anonymous" user is used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_preferences")
public class UserPreference {

    @Id
    private String id;

    /** User identifier (defaults to "default" for single-user mode) */
    @Indexed(unique = true)
    @Builder.Default
    private String userId = "default";

    /** Temperature units: "metric" (Celsius) or "imperial" (Fahrenheit) */
    @Builder.Default
    private String units = "metric";

    /** Auto-refresh interval in minutes (0 = disabled) */
    @Builder.Default
    private int refreshIntervalMinutes = 30;

    /** Wind speed unit preference */
    @Builder.Default
    private String windSpeedUnit = "kmh";

    /** Precipitation unit preference */
    @Builder.Default
    private String precipitationUnit = "mm";

    /** Default location ID to show on dashboard */
    private String defaultLocationId;

    /** Theme preference: "light" or "dark" */
    @Builder.Default
    private String theme = "dark";

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
