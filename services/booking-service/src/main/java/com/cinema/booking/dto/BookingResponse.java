package com.cinema.booking.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID bookingId,
        UUID reservationId,
        UUID showtimeId,
        String status,
        long amountMinor,
        String currency,
        Instant confirmedAt,
        List<ReservedSeat> seats) {
}
