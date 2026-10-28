package com.cinema.booking.messaging;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public record ReservationReleasedPayload(
        UUID reservationId,
        UUID bookingId,
        UUID showtimeId,
        List<UUID> seatIds,
        String reasonCode,
        Instant releasedAt) {
}
