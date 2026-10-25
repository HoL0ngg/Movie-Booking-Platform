package com.cinema.payment.dto;

import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        UUID paymentId,
        String status,
        long amountMinor,
        String currency,
        String failureCode,
        Instant createdAt,
        Instant updatedAt) {
}
