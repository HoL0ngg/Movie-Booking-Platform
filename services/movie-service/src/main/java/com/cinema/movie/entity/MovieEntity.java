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

    @Column(name = "poster_url", columnDefinition = "text")
    String posterUrl;

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
    public String getPosterUrl() { return posterUrl; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public String getStatus() { return status; }

    public static MovieEntity create() {
        MovieEntity movie = new MovieEntity();
        movie.id = UUID.randomUUID();
        movie.createdAt = Instant.now();
        return movie;
    }

    public void update(String title, String synopsis, String posterUrl, Integer durationMinutes,
                       LocalDate releaseDate, String status) {
        this.title = title.trim();
        this.synopsis = synopsis;
        this.posterUrl = posterUrl;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = "ARCHIVED";
        this.updatedAt = Instant.now();
    }
}
