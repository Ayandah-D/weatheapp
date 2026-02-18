package com.weatherapp.service;

import com.weatherapp.dto.WeatherDTO;
import com.weatherapp.exception.WeatherApiException;
import com.weatherapp.model.Location;
import com.weatherapp.model.WeatherSnapshot;
import com.weatherapp.repository.WeatherSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherService.
 * Tests weather data retrieval, storage, and conflict detection.
 */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherSnapshotRepository snapshotRepository;

    @Mock
    private WeatherApiClient weatherApiClient;

    @InjectMocks
    private WeatherService weatherService;

    private WeatherSnapshot sampleSnapshot;
    private Location sampleLocation;

    @BeforeEach
    void setUp() {
        sampleLocation = Location.builder()
                .id("loc-001")
                .name("Victoria Falls")
                .country("Zimbabwe")
                .latitude(-17.9243)
                .longitude(25.8572)
                .build();

        sampleSnapshot = WeatherSnapshot.builder()
                .id("snap-001")
                .locationId("loc-001")
                .current(WeatherSnapshot.CurrentWeather.builder()
                        .temperature(28.5)
                        .apparentTemperature(30.0)
                        .humidity(65.0)
                        .precipitation(0.0)
                        .weatherCode(2)
                        .weatherDescription("Partly cloudy")
                        .windSpeed(12.5)
                        .build())
                .dailyForecast(List.of(
                        WeatherSnapshot.DailyForecast.builder()
                                .date("2026-02-13")
                                .temperatureMax(32.0)
                                .temperatureMin(18.0)
                                .weatherCode(1)
                                .weatherDescription("Mainly clear")
                                .build()
                ))
                .hourlyForecast(List.of(
                        WeatherSnapshot.HourlyForecast.builder()
                                .time("2026-02-13T12:00")
                                .temperature(29.0)
                                .weatherCode(2)
                                .weatherDescription("Partly cloudy")
                                .build()
                ))
                .units("metric")
                .timezone("Africa/Harare")
                .fetchedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should return latest weather for a location")
    void getLatestWeather_success() {
        when(snapshotRepository.findTopByLocationIdOrderByFetchedAtDesc("loc-001"))
                .thenReturn(Optional.of(sampleSnapshot));

        WeatherDTO.SnapshotResponse response = weatherService.getLatestWeather("loc-001");

        assertThat(response).isNotNull();
        assertThat(response.getLocationId()).isEqualTo("loc-001");
        assertThat(response.getCurrent().getTemperature()).isEqualTo(28.5);
        assertThat(response.getCurrent().getWeatherDescription()).isEqualTo("Partly cloudy");
    }

    @Test
    @DisplayName("Should throw NotFoundException when no weather data exists")
    void getLatestWeather_notFound() {
        when(snapshotRepository.findTopByLocationIdOrderByFetchedAtDesc("loc-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> weatherService.getLatestWeather("loc-999"))
                .isInstanceOf(WeatherApiException.NotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should fetch and store new weather snapshot")
    void fetchAndStoreWeather_success() {
        WeatherSnapshot apiResult = WeatherSnapshot.builder()
                .current(WeatherSnapshot.CurrentWeather.builder()
                        .temperature(27.0)
                        .weatherCode(0)
                        .weatherDescription("Clear sky")
                        .build())
                .units("metric")
                .build();

        when(weatherApiClient.fetchWeatherData(anyDouble(), anyDouble(), anyString()))
                .thenReturn(apiResult);
        when(snapshotRepository.findTopByLocationIdOrderByFetchedAtDesc("loc-001"))
                .thenReturn(Optional.of(sampleSnapshot));
        when(snapshotRepository.save(any(WeatherSnapshot.class)))
                .thenAnswer(invocation -> {
                    WeatherSnapshot snap = invocation.getArgument(0);
                    snap.setId("snap-new");
                    return snap;
                });

        WeatherSnapshot result = weatherService.fetchAndStoreWeather(sampleLocation, "metric");

        assertThat(result).isNotNull();
        assertThat(result.getLocationId()).isEqualTo("loc-001");
        verify(snapshotRepository).save(any(WeatherSnapshot.class));
    }

    @Test
    @DisplayName("Should detect conflict when temperature changes significantly")
    void fetchAndStoreWeather_detectsConflict() {
        // Previous snapshot had 28.5, new data has 50.0 (>10 degree difference)
        WeatherSnapshot apiResult = WeatherSnapshot.builder()
                .current(WeatherSnapshot.CurrentWeather.builder()
                        .temperature(50.0)  // Significant change
                        .build())
                .units("metric")
                .build();

        when(weatherApiClient.fetchWeatherData(anyDouble(), anyDouble(), anyString()))
                .thenReturn(apiResult);
        when(snapshotRepository.findTopByLocationIdOrderByFetchedAtDesc("loc-001"))
                .thenReturn(Optional.of(sampleSnapshot));
        when(snapshotRepository.save(any(WeatherSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WeatherSnapshot result = weatherService.fetchAndStoreWeather(sampleLocation, "metric");

        // Conflict should be detected due to >10 degree change
        assertThat(result.isConflictDetected()).isTrue();
        assertThat(result.getConflictDescription()).contains("Temperature changed");
    }

    @Test
    @DisplayName("Should delegate city search to API client")
    void searchCities_delegatesToClient() {
        List<WeatherApiClient.GeocodingResult> mockResults = List.of(
                WeatherApiClient.GeocodingResult.builder()
                        .name("Victoria Falls")
                        .country("Zimbabwe")
                        .latitude(-17.9243)
                        .longitude(25.8572)
                        .build()
        );

        when(weatherApiClient.searchLocations("Victoria Falls"))
                .thenReturn(mockResults);

        List<WeatherApiClient.GeocodingResult> results = weatherService.searchCities("Victoria Falls");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Victoria Falls");
    }
}
