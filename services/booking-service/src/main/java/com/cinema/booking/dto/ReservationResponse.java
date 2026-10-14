package com.cinema.booking.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
                UUID reservationId,
                UUID showtimeId,
                String status,
                Instant holdExpiresAt,
                long totalMinor,
                List<ReservedSeat> seats) {
}
