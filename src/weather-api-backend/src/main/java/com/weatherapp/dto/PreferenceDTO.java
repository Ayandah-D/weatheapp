package com.weatherapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Objects for User Preference requests and responses.
 */
public class PreferenceDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String units;
        private Integer refreshIntervalMinutes;
        private String windSpeedUnit;
        private String precipitationUnit;
        private String defaultLocationId;
        private String theme;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String userId;
        private String units;
        private int refreshIntervalMinutes;
        private String windSpeedUnit;
        private String precipitationUnit;
        private String defaultLocationId;
        private String theme;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
