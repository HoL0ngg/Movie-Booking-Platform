package com.cinema.booking.dto;

import java.util.UUID;

public record ConfirmBookingRequest(UUID paymentId) {
}
