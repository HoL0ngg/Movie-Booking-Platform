package com.cinema.booking.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class BookingEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "reservation_id", nullable = false)
    UUID reservationId;

    @Column(name = "showtime_id", nullable = false)
    UUID showtimeId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "payment_id", nullable = false)
    UUID paymentId;

    @Column(name = "amount_minor", nullable = false)
    Long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "confirmed_at", columnDefinition = "timestamptz")
    Instant confirmedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public BookingEntity() {}
}
