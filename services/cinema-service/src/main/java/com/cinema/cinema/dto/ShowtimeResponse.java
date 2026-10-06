package com.cinema.cinema.dto; // 7.11

import java.time.Instant; // 7.12

public record ShowtimeResponse(String id, String movieId, String cinemaId, String auditoriumId, // 7.13 ID dạng chuỗi
                               Instant startsAt, Instant endsAt, Instant salesCloseAt, // 7.14 JSON dạng ISO-8601 UTC
                               MoneyResponse price, String status) { // 7.15
}   