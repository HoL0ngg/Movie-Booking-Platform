package com.cinema.cinema.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    public AuditoriumEntity(UUID id, UUID cinemaId, String name, Instant now) {
        this.id = id;
        this.cinemaId = cinemaId;
        this.createdAt = now;
        rename(name, now);
    }

    public void rename(String name, Instant now) {
        this.name = name;
        this.updatedAt = now;
    }

    public UUID getId() { return id; } // 15.6
    public UUID getCinemaId() { return cinemaId; } // 15.7
    public String getName() { return name; } // 15.8
}
