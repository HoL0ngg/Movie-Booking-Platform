package com.cinema.ai.client;

import java.util.UUID;

/** Public cinema read model from cinema-service {@code GET /api/v1/cinemas}. */
public record CatalogCinema(UUID id, String name, String address, String city, String timezone) {
}
