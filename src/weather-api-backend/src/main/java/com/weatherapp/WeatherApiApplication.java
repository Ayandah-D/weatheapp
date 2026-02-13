package com.weatherapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Weather Data Integration Platform.
 *
 * This application integrates with the Open-Meteo weather API,
 * stores data in MongoDB, and provides REST endpoints for
 * managing locations, weather snapshots, and user preferences.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableMongoAuditing
public class WeatherApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherApiApplication.class, args);
    }
}
