package com.cinema.payment.client;

import java.time.Instant;
import java.util.UUID;

/** Phần cần dùng của reservation lấy từ booking-service. */
public record ReservationSnapshot(UUID reservationId, String status, Instant holdExpiresAt, long totalMinor) {
}