package com.cinema.cinema.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "auditoriums")
public class AuditoriumEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "cinema_id", nullable = false)
    UUID cinemaId;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    String name;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public AuditoriumEntity() {}
}
