package com.cinema.payment.messaging;

import java.time.Instant;
import java.util.UUID;

public record RefundCompletedPayload(
        UUID refundId,
        UUID refundRequestId,
        UUID paymentId,
        UUID bookingId,
        UUID userId,
        long amountMinor,
        String currency,
        String providerRefundId,
        Instant refundedAt) {
}
