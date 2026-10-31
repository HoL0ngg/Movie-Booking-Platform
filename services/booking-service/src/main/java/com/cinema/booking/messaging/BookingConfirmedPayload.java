package com.cinema.booking.messaging;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedPayload(
                UUID bookingId,
                UUID reservationId,
                UUID showtimeId,
                UUID movieId,
                Instant startsAt,
                UUID userId,
                UUID paymentId,
                String paymentGateway,
                Instant confirmedAt,
                List<UUID> seatIds,
                List<SeatDetail> seatDetails,
                long amountMinor,
                String currency) {

    public record SeatDetail(UUID seatId, String seatLabel, long priceMinor) {
    }
}
