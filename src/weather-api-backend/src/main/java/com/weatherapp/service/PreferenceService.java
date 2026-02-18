package com.weatherapp.service;

import com.weatherapp.dto.PreferenceDTO;
import com.weatherapp.model.UserPreference;
import com.weatherapp.repository.UserPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Service for managing user preferences.
 * Provides get-or-create semantics: if no preferences exist,
 * default preferences are automatically created.
 */
@Service
public class PreferenceService {

    private static final Logger log = LoggerFactory.getLogger(PreferenceService.class);
    private static final String DEFAULT_USER_ID = "default";

    private final UserPreferenceRepository preferenceRepository;

    public PreferenceService(UserPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /**
     * Get preferences for the default user. Creates defaults if none exist.
     */
    public PreferenceDTO.Response getPreferences() {
        UserPreference prefs = getOrCreateDefaults();
        return toResponse(prefs);
    }

    /**
     * Update user preferences. Only non-null fields are updated.
     */
    public PreferenceDTO.Response updatePreferences(@NonNull PreferenceDTO.UpdateRequest request) {
        UserPreference prefs = getOrCreateDefaults();

        if (request.getUnits() != null) {
            prefs.setUnits(request.getUnits());
        }
        if (request.getRefreshIntervalMinutes() != null) {
            prefs.setRefreshIntervalMinutes(request.getRefreshIntervalMinutes());
        }
        if (request.getWindSpeedUnit() != null) {
            prefs.setWindSpeedUnit(request.getWindSpeedUnit());
        }
        if (request.getPrecipitationUnit() != null) {
            prefs.setPrecipitationUnit(request.getPrecipitationUnit());
        }
        if (request.getDefaultLocationId() != null) {
            prefs.setDefaultLocationId(request.getDefaultLocationId());
        }
        if (request.getTheme() != null) {
            prefs.setTheme(request.getTheme());
        }

        UserPreference saved = preferenceRepository.save(prefs);
        log.info("Preferences updated for user: {}", saved.getUserId());

        return toResponse(saved);
    }

    /**
     * Get the effective units setting for API calls.
     */
    public String getEffectiveUnits() {
        return getOrCreateDefaults().getUnits();
    }

    // --- Private helpers ---

    private UserPreference getOrCreateDefaults() {
        return preferenceRepository.findByUserId(DEFAULT_USER_ID)
                .orElseGet(() -> {
                    log.info("Creating default preferences for user: {}", DEFAULT_USER_ID);
                    UserPreference defaults = UserPreference.builder()
                            .userId(DEFAULT_USER_ID)
                            .units("metric")
                            .refreshIntervalMinutes(30)
                            .windSpeedUnit("kmh")
                            .precipitationUnit("mm")
                            .theme("dark")
                            .build();
                    return preferenceRepository.save(defaults);
                });
    }

    private PreferenceDTO.Response toResponse(UserPreference prefs) {
        return PreferenceDTO.Response.builder()
                .id(prefs.getId())
                .userId(prefs.getUserId())
                .units(prefs.getUnits())
                .refreshIntervalMinutes(prefs.getRefreshIntervalMinutes())
                .windSpeedUnit(prefs.getWindSpeedUnit())
                .precipitationUnit(prefs.getPrecipitationUnit())
                .defaultLocationId(prefs.getDefaultLocationId())
                .theme(prefs.getTheme())
                .createdAt(prefs.getCreatedAt())
                .updatedAt(prefs.getUpdatedAt())
                .build();
    }
}
