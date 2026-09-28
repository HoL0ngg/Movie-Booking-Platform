package com.cinema.movie.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "genres")
public class GenreEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    String name;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    public GenreEntity() {}
}
