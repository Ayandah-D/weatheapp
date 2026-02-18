package com.weatherapp.controller;

import com.weatherapp.dto.PreferenceDTO;
import com.weatherapp.service.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user preference management.
 *
 * Endpoints:
 * - GET  /api/preferences -> Get current user preferences
 * - PUT  /api/preferences -> Update user preferences
 */
@RestController
@RequestMapping("/api/preferences")
@Tag(name = "Preferences", description = "User preference management")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    @Operation(summary = "Get current user preferences")
    public ResponseEntity<PreferenceDTO.Response> getPreferences() {
        return ResponseEntity.ok(preferenceService.getPreferences());
    }

    @PutMapping
    @Operation(summary = "Update user preferences (units, refresh interval, theme)")
    public ResponseEntity<PreferenceDTO.Response> updatePreferences(
            @RequestBody @NonNull PreferenceDTO.UpdateRequest request) {
        return ResponseEntity.ok(preferenceService.updatePreferences(request));
    }
}
