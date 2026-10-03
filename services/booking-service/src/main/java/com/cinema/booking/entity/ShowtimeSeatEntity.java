package com.cinema.booking.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "showtime_seats")
@IdClass(ShowtimeSeatEntity.Key.class)
public class ShowtimeSeatEntity {
    @Id
    @Column(name = "showtime_id", nullable = false)
    UUID showtimeId;

    @Id
    @Column(name = "seat_id", nullable = false)
    UUID seatId;

    @Column(name = "seat_label", nullable = false, length = 32)
    String seatLabel;

    @Column(name = "seat_type", nullable = false, length = 24)
    String seatType;

    @Column(name = "price_minor", nullable = false)
    Long priceMinor;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "reservation_id")
    UUID reservationId;

    @Column(name = "booking_id")
    UUID bookingId;

    @Column(name = "hold_expires_at", columnDefinition = "timestamptz")
    Instant holdExpiresAt;

    @Version
    @Column(name = "version", nullable = false)
    Long version;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public ShowtimeSeatEntity() {}

    public static class Key implements Serializable {
        public UUID showtimeId;
        public UUID seatId;
        public Key() {}
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(showtimeId, key.showtimeId) && Objects.equals(seatId, key.seatId);
        }
        @Override public int hashCode() { return Objects.hash(showtimeId, seatId); }
    }
}
