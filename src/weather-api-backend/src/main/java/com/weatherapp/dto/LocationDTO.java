package com.weatherapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for Location requests and responses.
 */
public class LocationDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "City name is required")
        private String name;

        @NotBlank(message = "Country is required")
        private String country;

        @NotNull(message = "Latitude is required")
        private Double latitude;

        @NotNull(message = "Longitude is required")
        private Double longitude;

        private String displayName;
        private boolean favorite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String displayName;
        private Boolean favorite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String country;
        private Double latitude;
        private Double longitude;
        private String displayName;
        private boolean favorite;
        private Instant lastSyncAt;
        private String syncStatus;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WithWeatherResponse {
        private Response location;
        private WeatherDTO.SnapshotResponse currentWeather;
    }
}
