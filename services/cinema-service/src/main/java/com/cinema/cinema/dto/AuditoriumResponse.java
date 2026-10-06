package com.cinema.cinema.dto; // 7.3

public record AuditoriumResponse(String id, String name, long seatCount) { // 7.4 seatCount = số ghế đang hoạt động (không phải ghế trống)
}