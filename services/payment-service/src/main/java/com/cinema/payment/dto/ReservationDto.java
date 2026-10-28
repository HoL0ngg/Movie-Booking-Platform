package com.cinema.payment.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationDto(UUID reservationId, String status, String holdExpiresAt, long totalMinor) {
}
