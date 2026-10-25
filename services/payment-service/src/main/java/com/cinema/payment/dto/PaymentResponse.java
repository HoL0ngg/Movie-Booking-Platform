package com.cinema.payment.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái thanh toán. checkout chỉ có khi còn có thể thanh toán.
 * Kết quả cuối cùng chỉ do IPN/querydr (server-to-server) quyết định, không
 * phải redirect của trình duyệt.
 */
public record PaymentResponse(
        UUID paymentId,
        UUID bookingId,
        UUID reservationId,
        String status,
        long amountMinor,
        String currency,
        Checkout checkout,
        String failureCode,
        Instant createdAt,
        Instant updatedAt) {

    public record Checkout(String provider, String url) {
    }
}
