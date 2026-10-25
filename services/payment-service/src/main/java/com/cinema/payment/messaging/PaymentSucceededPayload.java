package com.cinema.payment.messaging;

import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededPayload(
        UUID paymentId,
        UUID bookingId,
        UUID reservationId,
        long amountMinor,
        String currency,
        String provider,
        String providerTransactionId,
        Instant succeededAt) {
}
