package com.cinema.movie.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AdminMovieRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 3000) String synopsis,
        @Size(max = 2048) @Pattern(regexp = "(?i)^https?://[^\\s/]+(?:/[^\\s]*)?$", message = "Use an HTTP(S) poster URL") String posterUrl,
        @NotNull @Min(1) @Max(600) Integer durationMinutes,
        LocalDate releaseDate,
        @NotNull @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED") String status,
        @NotNull @Size(min = 1, max = 50) List<@NotNull UUID> genreIds) {}
