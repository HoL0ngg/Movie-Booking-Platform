package com.cinema.booking.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "booking_items")
public class BookingItemEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "booking_id", nullable = false)
    UUID bookingId;

    @Column(name = "showtime_id", nullable = false)
    UUID showtimeId;

    @Column(name = "seat_id", nullable = false)
    UUID seatId;

    @Column(name = "price_minor", nullable = false)
    Long priceMinor;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    public BookingItemEntity() {}
}
