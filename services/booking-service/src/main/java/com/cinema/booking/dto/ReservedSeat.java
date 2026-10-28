package com.cinema.booking.dto;

import java.util.UUID;

public record ReservedSeat(UUID seatId, String seatLabel, long priceMinor) {

}
