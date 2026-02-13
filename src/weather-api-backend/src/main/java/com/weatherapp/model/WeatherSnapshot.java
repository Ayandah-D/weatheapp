package com.weatherapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Stores a snapshot of weather data fetched from the API for a given location.
 * Includes current conditions and forecast data.
 * Historical snapshots are preserved for trend analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "weather_snapshots")
@CompoundIndex(name = "location_fetched_idx", def = "{'locationId': 1, 'fetchedAt': -1}")
public class WeatherSnapshot {

    @Id
    private String id;

    /** Reference to the Location document */
    @Indexed
    private String locationId;

    /** Current weather conditions at time of fetch */
    private CurrentWeather current;

    /** Hourly forecast data */
    private List<HourlyForecast> hourlyForecast;

    /** Daily forecast data */
    private List<DailyForecast> dailyForecast;

    /** Units used when fetching (metric or imperial) */
    private String units;

    /** Timezone of the location */
    private String timezone;

    /** When this data was fetched from the API */
    @CreatedDate
    private Instant fetchedAt;

    /** Whether a conflict was detected with previous data */
    @Builder.Default
    private boolean conflictDetected = false;

    /** Description of the conflict if detected */
    private String conflictDescription;

    // --- Embedded sub-documents ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentWeather {
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
    public static class HourlyForecast {
        private String time;
        private Double temperature;
        private Integer weatherCode;
        private String weatherDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private String date;
        private Double temperatureMax;
        private Double temperatureMin;
        private Integer weatherCode;
        private String weatherDescription;
    }
}
