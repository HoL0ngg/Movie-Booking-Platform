package com.cinema.ai.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Read-only catalog query settings. Data is fetched from the owning services' public
 * APIs (movie-service, cinema-service); ai-service never reads their databases (ADR-009/ADR-014).
 */
@ConfigurationProperties(prefix = "ai.catalog")
public record CatalogProperties(
        @DefaultValue("http://localhost:8082") String movieServiceUrl,
        @DefaultValue("http://localhost:8083") String cinemaServiceUrl,
        @DefaultValue("Asia/Ho_Chi_Minh") String timezone,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("2s") Duration readTimeout,
        @DefaultValue("40") int maxMovies,
        @DefaultValue("80") int maxShowtimes) {
}
