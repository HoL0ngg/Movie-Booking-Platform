package com.cinema.payment.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Đã thu tiền nhưng booking-service không xác nhận được booking => cần hoàn
 * tiền.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingConfirmationFailedPayload(UUID reservationId, UUID paymentId, String reason) {
}
