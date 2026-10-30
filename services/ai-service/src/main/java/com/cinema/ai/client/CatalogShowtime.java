package com.cinema.ai.client;

import java.time.Instant;
import java.util.UUID;

/** Published showtime read model from cinema-service {@code GET /api/v1/showtimes?date=...}. */
public record CatalogShowtime(UUID id, UUID movieId, UUID cinemaId, Instant startsAt, Instant endsAt, Money price,
        String status) {

    public record Money(long amountMinor, String currency) {
    }
}
