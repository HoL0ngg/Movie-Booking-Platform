package com.cinema.booking.messaging;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedPayload(
        UUID bookingId,
        UUID reservationId,
        UUID showtimeId,
        UUID userId,
        UUID paymentId,
        Instant confirmedAt,
        List<UUID> seatIds) {
}
