package com.weatherapp.service;

import com.weatherapp.dto.LocationDTO;
import com.weatherapp.exception.WeatherApiException;
import com.weatherapp.model.Location;
import com.weatherapp.repository.LocationRepository;
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
 * Unit tests for LocationService.
 * Tests CRUD operations, validation, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WeatherSnapshotRepository weatherSnapshotRepository;

    @InjectMocks
    private LocationService locationService;

    private Location sampleLocation;
    private LocationDTO.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleLocation = Location.builder()
                .id("loc-001")
                .name("Victoria Falls")
                .country("Zimbabwe")
                .latitude(-17.9243)
                .longitude(25.8572)
                .favorite(true)
                .syncStatus(Location.SyncStatus.NEVER_SYNCED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = LocationDTO.CreateRequest.builder()
                .name("Cape Town")
                .country("South Africa")
                .latitude(-33.9249)
                .longitude(18.4241)
                .favorite(false)
                .build();
    }

    @Test
    @DisplayName("CREATE - Should successfully add a new location")
    void createLocation_success() {
        when(locationRepository.existsByNameIgnoreCaseAndCountryIgnoreCase(anyString(), anyString()))
                .thenReturn(false);
        when(locationRepository.save(any(Location.class)))
                .thenAnswer(invocation -> {
                    Location loc = invocation.getArgument(0);
                    loc.setId("new-id");
                    return loc;
                });

        LocationDTO.Response response = locationService.createLocation(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Cape Town");
        assertThat(response.getCountry()).isEqualTo("South Africa");
        assertThat(response.getSyncStatus()).isEqualTo("NEVER_SYNCED");
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    @DisplayName("CREATE - Should throw DuplicateException for duplicate city")
    void createLocation_duplicate() {
        when(locationRepository.existsByNameIgnoreCaseAndCountryIgnoreCase(anyString(), anyString()))
                .thenReturn(true);

        assertThatThrownBy(() -> locationService.createLocation(createRequest))
                .isInstanceOf(WeatherApiException.DuplicateException.class)
                .hasMessageContaining("already exists");

        verify(locationRepository, never()).save(any());
    }

    @Test
    @DisplayName("READ - Should return all locations ordered by name")
    void getAllLocations_success() {
        Location loc2 = Location.builder()
                .id("loc-002")
                .name("Cape Town")
                .country("South Africa")
                .latitude(-33.9249)
                .longitude(18.4241)
                .syncStatus(Location.SyncStatus.SUCCESS)
                .build();

        when(locationRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(loc2, sampleLocation));

        List<LocationDTO.Response> result = locationService.getAllLocations();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Cape Town");
        assertThat(result.get(1).getName()).isEqualTo("Victoria Falls");
    }

    @Test
    @DisplayName("READ - Should return location by ID")
    void getLocationById_success() {
        when(locationRepository.findById("loc-001"))
                .thenReturn(Optional.of(sampleLocation));

        LocationDTO.Response response = locationService.getLocationById("loc-001");

        assertThat(response.getId()).isEqualTo("loc-001");
        assertThat(response.getName()).isEqualTo("Victoria Falls");
    }

    @Test
    @DisplayName("READ - Should throw NotFoundException for invalid ID")
    void getLocationById_notFound() {
        when(locationRepository.findById("invalid-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getLocationById("invalid-id"))
                .isInstanceOf(WeatherApiException.NotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("UPDATE - Should update location favorite status")
    void updateLocation_favorite() {
        when(locationRepository.findById("loc-001"))
                .thenReturn(Optional.of(sampleLocation));
        when(locationRepository.save(any(Location.class)))
                .thenReturn(sampleLocation);

        LocationDTO.UpdateRequest updateRequest = LocationDTO.UpdateRequest.builder()
                .favorite(false)
                .build();

        LocationDTO.Response response = locationService.updateLocation("loc-001", updateRequest);

        assertThat(response).isNotNull();
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    @DisplayName("UPDATE - Should update display name")
    void updateLocation_displayName() {
        when(locationRepository.findById("loc-001"))
                .thenReturn(Optional.of(sampleLocation));
        when(locationRepository.save(any(Location.class)))
                .thenReturn(sampleLocation);

        LocationDTO.UpdateRequest updateRequest = LocationDTO.UpdateRequest.builder()
                .displayName("Vic Falls")
                .build();

        locationService.updateLocation("loc-001", updateRequest);

        verify(locationRepository).save(argThat(loc ->
                "Vic Falls".equals(loc.getDisplayName())));
    }

    @Test
    @DisplayName("DELETE - Should delete location and cascade to snapshots")
    void deleteLocation_cascadesSnapshots() {
        when(locationRepository.findById("loc-001"))
                .thenReturn(Optional.of(sampleLocation));
        when(weatherSnapshotRepository.countByLocationId("loc-001"))
                .thenReturn(5L);

        locationService.deleteLocation("loc-001");

        verify(weatherSnapshotRepository).deleteByLocationId("loc-001");
        verify(locationRepository).delete(sampleLocation);
    }

    @Test
    @DisplayName("DELETE - Should throw NotFoundException for invalid ID")
    void deleteLocation_notFound() {
        when(locationRepository.findById("invalid-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.deleteLocation("invalid-id"))
                .isInstanceOf(WeatherApiException.NotFoundException.class);

        verify(locationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return only favorite locations")
    void getFavoriteLocations() {
        when(locationRepository.findByFavoriteTrueOrderByNameAsc())
                .thenReturn(List.of(sampleLocation));

        List<LocationDTO.Response> result = locationService.getFavoriteLocations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isFavorite()).isTrue();
    }
}
