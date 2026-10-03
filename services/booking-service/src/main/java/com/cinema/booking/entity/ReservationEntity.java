package com.cinema.booking.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
public class ReservationEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "showtime_id", nullable = false)
    UUID showtimeId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "hold_expires_at", nullable = false, columnDefinition = "timestamptz")
    Instant holdExpiresAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public ReservationEntity() {}
}
