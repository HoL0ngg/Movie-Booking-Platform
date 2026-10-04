package com.cinema.movie.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MovieResponse(UUID id, String title, String synopsis, String posterUrl,
                            Integer durationMinutes, LocalDate releaseDate, String status) {}
