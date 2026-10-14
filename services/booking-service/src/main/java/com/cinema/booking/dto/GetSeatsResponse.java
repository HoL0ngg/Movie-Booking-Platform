package com.cinema.booking.dto;

import java.util.UUID;

public record GetSeatsResponse(
                UUID showtimeId,
                UUID seatId,
                String seatLabel,
                String seatType,
                Long priceMinor,
                String status) {
}
