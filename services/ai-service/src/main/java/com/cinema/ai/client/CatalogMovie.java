package com.cinema.ai.client;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Public movie read model from movie-service {@code GET /api/v1/movies}. */
public record CatalogMovie(UUID id, String title, Integer durationMinutes, LocalDate releaseDate, String status,
        List<String> genres) {

    public CatalogMovie {
        genres = genres == null ? List.of() : List.copyOf(genres);
    }
}
