package com.cinema.movie.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminMovieResponse(UUID id, String title, String synopsis, String posterUrl,
                                 Integer durationMinutes, LocalDate releaseDate, String status, List<UUID> genreIds) {}
