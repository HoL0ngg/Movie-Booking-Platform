package com.cinema.booking.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "reservation_seats")
@IdClass(ReservationSeatEntity.Key.class)
public class ReservationSeatEntity {
    @Id
    @Column(name = "reservation_id", nullable = false)
    UUID reservationId;

    @Column(name = "showtime_id", nullable = false)
    UUID showtimeId;

    @Id
    @Column(name = "seat_id", nullable = false)
    UUID seatId;

    @Column(name = "price_minor", nullable = false)
    Long priceMinor;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    public ReservationSeatEntity() {}

    public static class Key implements Serializable {
        public UUID reservationId;
        public UUID seatId;
        public Key() {}
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(reservationId, key.reservationId) && Objects.equals(seatId, key.seatId);
        }
        @Override public int hashCode() { return Objects.hash(reservationId, seatId); }
    }
}
