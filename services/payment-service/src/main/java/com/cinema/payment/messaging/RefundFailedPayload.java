package com.cinema.payment.messaging;

import java.time.Instant;
import java.util.UUID;

public record RefundFailedPayload(
        UUID refundId,
        UUID refundRequestId,
        UUID paymentId,
        String failureCode,
        Instant failedAt,
        boolean retryable) {
}
