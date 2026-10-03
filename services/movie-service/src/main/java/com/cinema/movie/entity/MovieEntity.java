package com.cinema.movie.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "movies", schema = "movie")
public class MovieEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "title", nullable = false, columnDefinition = "text")
    String title;

    @Column(name = "synopsis", columnDefinition = "text")
    String synopsis;

    @Column(name = "duration_minutes", nullable = false)
    Integer durationMinutes;

    @Column(name = "release_date")
    LocalDate releaseDate;

    @Column(name = "status", nullable = false, length = 16)
    String status;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public MovieEntity() {}

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getSynopsis() { return synopsis; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public LocalDate getReleaseDate() { return releaseDate; }
}
