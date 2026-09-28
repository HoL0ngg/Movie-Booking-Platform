package com.cinema.movie.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "movie_genres")
@IdClass(MovieGenreEntity.Key.class)
public class MovieGenreEntity {
    @Id
    @Column(name = "movie_id", nullable = false)
    UUID movieId;

    @Id
    @Column(name = "genre_id", nullable = false)
    UUID genreId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    public MovieGenreEntity() {}

    public static class Key implements Serializable {
        public UUID movieId;
        public UUID genreId;
        public Key() {}
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(movieId, key.movieId) && Objects.equals(genreId, key.genreId);
        }
        @Override public int hashCode() { return Objects.hash(movieId, genreId); }
    }
}
