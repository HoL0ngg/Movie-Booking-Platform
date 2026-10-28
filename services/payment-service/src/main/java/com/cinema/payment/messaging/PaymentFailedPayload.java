package com.cinema.payment.messaging;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedPayload(
        UUID paymentId,
        UUID bookingId,
        UUID reservationId,
        String provider,
        String failureCode,
        Instant failedAt) {
}
