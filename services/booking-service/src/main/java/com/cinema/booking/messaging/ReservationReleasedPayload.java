package com.cinema.booking.messaging;

import java.util.List;
import java.util.UUID;

public record ReservationReleasedPayload(
        UUID reservationId,
        UUID showtimeId,
        UUID userId,
        String reason,
        List<UUID> seatIds) {
}
