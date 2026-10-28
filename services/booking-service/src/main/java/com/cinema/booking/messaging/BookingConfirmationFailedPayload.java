package com.cinema.booking.messaging;

import java.util.UUID;

/**
 * Đã thanh toán nhưng không xác nhận được booking => payment service cần hoàn
 * tiền.
 */
public record BookingConfirmationFailedPayload(UUID reservationId, UUID paymentId, String reason) {
}
