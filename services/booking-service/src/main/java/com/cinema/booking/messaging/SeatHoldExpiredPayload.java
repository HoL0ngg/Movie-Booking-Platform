package com.cinema.booking.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SeatHoldExpiredPayload(
        UUID reservationId,
        UUID userId,
        UUID showtimeId,
        List<UUID> seatIds,
        Instant expiredAt) {
}
