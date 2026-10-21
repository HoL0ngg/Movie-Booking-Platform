package com.cinema.payment.messaging;

import java.util.UUID;

/**
 * Payload chung của PaymentSucceeded / PaymentFailed / PaymentRefunded /
 * PaymentRefundFailed.
 * booking-service chỉ đọc paymentId + reservationId (bỏ qua field lạ), các
 * field còn lại để dùng cho consumer khác.
 */
public record PaymentPayload(
        UUID paymentId,
        UUID reservationId,
        UUID userId,
        long amountMinor,
        String currency,
        String provider,
        String providerTransactionId,
        String reasonCode) {
}