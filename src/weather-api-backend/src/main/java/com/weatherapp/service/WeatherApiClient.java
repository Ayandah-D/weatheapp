package com.weatherapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.weatherapp.exception.WeatherApiException;
import com.weatherapp.model.WeatherSnapshot;
import com.weatherapp.util.WeatherCodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Client service for the Open-Meteo Weather API.
 * Handles fetching current weather and forecast data,
 * and geocoding city names to coordinates.
 *
 * Uses WebClient for non-blocking HTTP calls with error handling
 * for rate limits, invalid cities, and network failures.
 */
@Service
public class WeatherApiClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiClient.class);

    private final WebClient weatherClient;
    private final WebClient geocodingClient;

    public WeatherApiClient(
            @Value("${weather.api.base-url}") String weatherBaseUrl,
            @Value("${weather.api.geocoding-url}") String geocodingBaseUrl) {
        this.weatherClient = WebClient.builder()
                .baseUrl(weatherBaseUrl)
                .build();
        this.geocodingClient = WebClient.builder()
                .baseUrl(geocodingBaseUrl)
                .build();
    }

    /**
     * Fetch current weather + forecast data from Open-Meteo for given coordinates.
     *
     * @param lat   Latitude
     * @param lon   Longitude
     * @param units "metric" or "imperial"
     * @return Parsed weather data ready for storage
     */
    public WeatherSnapshot fetchWeatherData(double lat, double lon, String units) {
        String tempUnit = "metric".equals(units) ? "celsius" : "fahrenheit";
        String windUnit = "metric".equals(units) ? "kmh" : "mph";
        String precipUnit = "metric".equals(units) ? "mm" : "inch";

        log.info("Fetching weather data for lat={}, lon={}, units={}", lat, lon, units);

        try {
            JsonNode response = weatherClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m")
                            .queryParam("hourly", "temperature_2m,weather_code")
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min")
                            .queryParam("temperature_unit", tempUnit)
                            .queryParam("wind_speed_unit", windUnit)
                            .queryParam("precipitation_unit", precipUnit)
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode().value() == 429) {
                            return Mono.error(new WeatherApiException.RateLimitException());
                        }
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new WeatherApiException.ExternalApiException("Open-Meteo", "Client error: " + body)));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new WeatherApiException.ExternalApiException("Open-Meteo", "Server error: " + body))))
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                throw new WeatherApiException.ExternalApiException("Open-Meteo", "Empty response received");
            }

            return parseWeatherResponse(response, units);

        } catch (WeatherApiException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Network error fetching weather data: {}", e.getMessage());
            throw new WeatherApiException.ExternalApiException("Open-Meteo", "Network failure: " + e.getMessage(), e);
        }
    }

    /**
     * Search for locations using the Open-Meteo Geocoding API.
     *
     * @param cityName The city name to search for
     * @return List of matching location results
     */
    @Cacheable(value = "geocoding", key = "#cityName.toLowerCase()")
    public List<GeocodingResult> searchLocations(String cityName) {
        log.info("Searching locations for: {}", cityName);

        try {
            JsonNode response = geocodingClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("name", cityName)
                            .queryParam("count", 10)
                            .queryParam("language", "en")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new WeatherApiException.ExternalApiException("Open-Meteo Geocoding", body))))
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("results")) {
                throw new WeatherApiException.InvalidCityException(cityName);
            }

            List<GeocodingResult> results = new ArrayList<>();
            JsonNode resultsNode = response.get("results");
            for (JsonNode node : resultsNode) {
                results.add(GeocodingResult.builder()
                        .name(getTextValue(node, "name"))
                        .country(getTextValue(node, "country"))
                        .countryCode(getTextValue(node, "country_code"))
                        .latitude(node.has("latitude") ? node.get("latitude").asDouble() : 0)
                        .longitude(node.has("longitude") ? node.get("longitude").asDouble() : 0)
                        .admin1(getTextValue(node, "admin1"))
                        .build());
            }
            return results;

        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching locations: {}", e.getMessage());
            throw new WeatherApiException.ExternalApiException("Open-Meteo Geocoding", "Search failed: " + e.getMessage(), e);
        }
    }

    // --- Private parsing helpers ---

    private WeatherSnapshot parseWeatherResponse(JsonNode response, String units) {
        JsonNode currentNode = response.get("current");

        WeatherSnapshot.CurrentWeather current = WeatherSnapshot.CurrentWeather.builder()
                .temperature(getDoubleValue(currentNode, "temperature_2m"))
                .apparentTemperature(getDoubleValue(currentNode, "apparent_temperature"))
                .humidity(getDoubleValue(currentNode, "relative_humidity_2m"))
                .precipitation(getDoubleValue(currentNode, "precipitation"))
                .weatherCode(getIntValue(currentNode, "weather_code"))
                .weatherDescription(WeatherCodeMapper.getDescription(getIntValue(currentNode, "weather_code")))
                .windSpeed(getDoubleValue(currentNode, "wind_speed_10m"))
                .build();

        // Parse hourly forecast
        List<WeatherSnapshot.HourlyForecast> hourly = new ArrayList<>();
        if (response.has("hourly")) {
            JsonNode hourlyNode = response.get("hourly");
            JsonNode times = hourlyNode.get("time");
            JsonNode temps = hourlyNode.get("temperature_2m");
            JsonNode codes = hourlyNode.get("weather_code");

            if (times != null && temps != null) {
                for (int i = 0; i < times.size(); i++) {
                    hourly.add(WeatherSnapshot.HourlyForecast.builder()
                            .time(times.get(i).asText())
                            .temperature(temps.get(i).asDouble())
                            .weatherCode(codes != null && i < codes.size() ? codes.get(i).asInt() : null)
                            .weatherDescription(codes != null && i < codes.size()
                                    ? WeatherCodeMapper.getDescription(codes.get(i).asInt()) : "Unknown")
                            .build());
                }
            }
        }

        // Parse daily forecast
        List<WeatherSnapshot.DailyForecast> daily = new ArrayList<>();
        if (response.has("daily")) {
            JsonNode dailyNode = response.get("daily");
            JsonNode dates = dailyNode.get("time");
            JsonNode maxTemps = dailyNode.get("temperature_2m_max");
            JsonNode minTemps = dailyNode.get("temperature_2m_min");
            JsonNode codes = dailyNode.get("weather_code");

            if (dates != null && maxTemps != null && minTemps != null) {
                for (int i = 0; i < dates.size(); i++) {
                    daily.add(WeatherSnapshot.DailyForecast.builder()
                            .date(dates.get(i).asText())
                            .temperatureMax(maxTemps.get(i).asDouble())
                            .temperatureMin(minTemps.get(i).asDouble())
                            .weatherCode(codes != null && i < codes.size() ? codes.get(i).asInt() : null)
                            .weatherDescription(codes != null && i < codes.size()
                                    ? WeatherCodeMapper.getDescription(codes.get(i).asInt()) : "Unknown")
                            .build());
                }
            }
        }

        String timezone = response.has("timezone") ? response.get("timezone").asText() : "UTC";

        return WeatherSnapshot.builder()
                .current(current)
                .hourlyForecast(hourly)
                .dailyForecast(daily)
                .units(units)
                .timezone(timezone)
                .build();
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }

    private Double getDoubleValue(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
    }

    // --- Inner result class ---

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GeocodingResult {
        private String name;
        private String country;
        private String countryCode;
        private double latitude;
        private double longitude;
        private String admin1; // State/Province
    }
}
