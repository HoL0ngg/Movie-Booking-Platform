package com.cinema.booking.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Payload của PaymentSucceeded / PaymentFailed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentPayload(UUID paymentId, UUID reservationId) {
}
