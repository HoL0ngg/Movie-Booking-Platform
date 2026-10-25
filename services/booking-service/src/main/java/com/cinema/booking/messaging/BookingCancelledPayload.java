package com.cinema.booking.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingCancelledPayload(
        UUID bookingId,
        UUID reservationId,
        UUID paymentId,
        UUID userId,
        UUID showtimeId,
        List<UUID> seatIds,
        String reasonCode,
        Instant cancelledAt) {
}
