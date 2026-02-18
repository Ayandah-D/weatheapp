package com.weatherapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weatherapp.dto.LocationDTO;
import com.weatherapp.exception.GlobalExceptionHandler;
import com.weatherapp.exception.WeatherApiException;
import com.weatherapp.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LocationController.
 * Tests HTTP request/response handling, validation, and error responses.
 */
@WebMvcTest(LocationController.class)
@Import(GlobalExceptionHandler.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @Test
    @DisplayName("POST /api/locations - Should create a new location")
    void createLocation_returns201() throws Exception {
        LocationDTO.CreateRequest request = LocationDTO.CreateRequest.builder()
                .name("Cape Town")
                .country("South Africa")
                .latitude(-33.9249)
                .longitude(18.4241)
                .build();

        LocationDTO.Response response = LocationDTO.Response.builder()
                .id("new-id")
                .name("Cape Town")
                .country("South Africa")
                .latitude(-33.9249)
                .longitude(18.4241)
                .syncStatus("NEVER_SYNCED")
                .createdAt(Instant.now())
                .build();

        when(locationService.createLocation(any())).thenReturn(response);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cape Town"))
                .andExpect(jsonPath("$.country").value("South Africa"))
                .andExpect(jsonPath("$.syncStatus").value("NEVER_SYNCED"));
    }

    @Test
    @DisplayName("POST /api/locations - Should return 400 for invalid request")
    void createLocation_validationError() throws Exception {
        // Missing required fields
        String invalidJson = "{\"latitude\": -33.92}";

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/locations - Should return all locations")
    void getAllLocations_returns200() throws Exception {
        List<LocationDTO.Response> locations = List.of(
                LocationDTO.Response.builder()
                        .id("1")
                        .name("Cape Town")
                        .country("South Africa")
                        .syncStatus("SUCCESS")
                        .build(),
                LocationDTO.Response.builder()
                        .id("2")
                        .name("Victoria Falls")
                        .country("Zimbabwe")
                        .syncStatus("NEVER_SYNCED")
                        .build()
        );

        when(locationService.getAllLocations()).thenReturn(locations);

        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Cape Town"));
    }

    @Test
    @DisplayName("GET /api/locations/{id} - Should return 404 for unknown ID")
    void getLocationById_returns404() throws Exception {
        when(locationService.getLocationById("unknown"))
                .thenThrow(new WeatherApiException.NotFoundException("Location", "unknown"));

        mockMvc.perform(get("/api/locations/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/locations/{id} - Should return 204 on success")
    void deleteLocation_returns204() throws Exception {
        doNothing().when(locationService).deleteLocation("loc-001");

        mockMvc.perform(delete("/api/locations/loc-001"))
                .andExpect(status().isNoContent());

        verify(locationService).deleteLocation("loc-001");
    }

    @Test
    @DisplayName("POST /api/locations - Should return 409 for duplicate location")
    void createLocation_duplicate_returns409() throws Exception {
        LocationDTO.CreateRequest request = LocationDTO.CreateRequest.builder()
                .name("Cape Town")
                .country("South Africa")
                .latitude(-33.9249)
                .longitude(18.4241)
                .build();

        when(locationService.createLocation(any()))
                .thenThrow(new WeatherApiException.DuplicateException("Location", "Cape Town, South Africa"));

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }
}
