package com.cinema.booking.messaging;

import java.time.Instant;
import java.util.UUID;

public record PaymentRequestedPayload(
        UUID paymentId,
        UUID bookingId,
        UUID reservationId,
        UUID userId,
        long amountMinor,
        String currency,
        String idempotencyKey,
        Instant requestedAt,
        Instant holdExpiresAt,
        String provider,
        String bankCode,
        String locale,
        String clientIp) {
}
