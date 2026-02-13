package com.weatherapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception hierarchy for the Weather API application.
 * Provides structured error handling with HTTP status codes.
 */
public class WeatherApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public WeatherApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public WeatherApiException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // --- Specific exception subtypes ---

    /** Thrown when a requested resource is not found */
    public static class NotFoundException extends WeatherApiException {
        public NotFoundException(String resource, String identifier) {
            super(
                String.format("%s not found with identifier: %s", resource, identifier),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
            );
        }
    }

    /** Thrown when a duplicate resource is detected */
    public static class DuplicateException extends WeatherApiException {
        public DuplicateException(String resource, String identifier) {
            super(
                String.format("%s already exists: %s", resource, identifier),
                HttpStatus.CONFLICT,
                "DUPLICATE_RESOURCE"
            );
        }
    }

    /** Thrown when an external API call fails */
    public static class ExternalApiException extends WeatherApiException {
        public ExternalApiException(String apiName, String message) {
            super(
                String.format("External API error (%s): %s", apiName, message),
                HttpStatus.BAD_GATEWAY,
                "EXTERNAL_API_ERROR"
            );
        }

        public ExternalApiException(String apiName, String message, Throwable cause) {
            super(
                String.format("External API error (%s): %s", apiName, message),
                HttpStatus.BAD_GATEWAY,
                "EXTERNAL_API_ERROR",
                cause
            );
        }
    }

    /** Thrown when rate limit is exceeded */
    public static class RateLimitException extends WeatherApiException {
        public RateLimitException() {
            super(
                "Rate limit exceeded. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED"
            );
        }
    }

    /** Thrown when an invalid city name is provided */
    public static class InvalidCityException extends WeatherApiException {
        public InvalidCityException(String cityName) {
            super(
                String.format("Invalid or unrecognized city: %s", cityName),
                HttpStatus.BAD_REQUEST,
                "INVALID_CITY"
            );
        }
    }
}
