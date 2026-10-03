package com.cinema.cinema.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cinemas")
public class CinemaEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    String name;

    @Column(name = "address", nullable = false, columnDefinition = "text")
    String address;

    @Column(name = "city", nullable = false, columnDefinition = "text")
    String city;

    @Column(name = "timezone", nullable = false, columnDefinition = "text")
    String timezone;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public CinemaEntity() {}
}
