package com.weatherapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Objects for Weather data requests and responses.
 */
public class WeatherDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotResponse {
        private String id;
        private String locationId;
        private CurrentWeatherResponse current;
        private List<HourlyForecastResponse> hourlyForecast;
        private List<DailyForecastResponse> dailyForecast;
        private String units;
        private String timezone;
        private Instant fetchedAt;
        private boolean conflictDetected;
        private String conflictDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentWeatherResponse {
        private Double temperature;
        private Double apparentTemperature;
        private Double humidity;
        private Double precipitation;
        private Integer weatherCode;
        private String weatherDescription;
        private Double windSpeed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyForecastResponse {
        private String time;
        private Double temperature;
        private Integer weatherCode;
        private String weatherDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecastResponse {
        private String date;
        private Double temperatureMax;
        private Double temperatureMin;
        private Integer weatherCode;
        private String weatherDescription;
    }

    /** Response for sync operations */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResponse {
        private String locationId;
        private String locationName;
        private boolean success;
        private String message;
        private Instant syncedAt;
        private boolean conflictDetected;
        private String conflictDescription;
    }
}
