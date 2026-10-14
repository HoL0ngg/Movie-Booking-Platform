package com.cinema.booking.dto;

import java.util.List;
import java.util.UUID;

public record CreateReservationRequest(UUID showtimeId, List<UUID> seatIds) {
}
