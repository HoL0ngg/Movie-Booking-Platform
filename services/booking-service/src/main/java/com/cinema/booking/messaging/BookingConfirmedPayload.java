package com.cinema.booking.messaging;

import java.util.List;
import java.util.UUID;

public record BookingConfirmedPayload(
        UUID bookingId,
        UUID reservationId,
        UUID showtimeId,
        UUID userId,
        UUID paymentId,
        long amountMinor,
        String currency,
        String confirmedAt,
        List<UUID> seatIds) {
}
