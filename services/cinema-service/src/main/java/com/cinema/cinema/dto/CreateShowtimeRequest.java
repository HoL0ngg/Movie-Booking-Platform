package com.cinema.cinema.dto; // 7.16

import java.time.Instant; // 7.17
import java.util.UUID; // 7.18

import jakarta.validation.constraints.NotNull; // 7.19
import jakarta.validation.constraints.Pattern; // 7.20
import jakarta.validation.constraints.PositiveOrZero; // 7.21

public record CreateShowtimeRequest( // 7.22
        @NotNull UUID auditoriumId, // 7.23 Bắt buộc
        @NotNull UUID movieId, // 7.24 Tham chiếu logic sang movie-service
        @NotNull Instant startsAt, // 7.25
        @NotNull Instant endsAt, // 7.26
        @NotNull Instant salesCloseAt, // 7.27
        @NotNull @PositiveOrZero Long priceMinor, // 7.28 Long (không phải long) để phát hiện thiếu field
        @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency) { // 7.29 Mã tiền tệ 3 chữ in hoa (khớp CHECK ở DB)
}