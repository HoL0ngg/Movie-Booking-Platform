package com.cinema.cinema.controller; // 5.1

import com.cinema.cinema.dto.ShowtimeResponse; // 5.2
import com.cinema.cinema.service.ShowtimeService; // 5.3
import java.time.LocalDate; // 5.4
import java.util.List; // 5.5
import java.util.UUID; // 5.6
import org.springframework.format.annotation.DateTimeFormat; // 5.7 Chỉ định định dạng ngày
import org.springframework.web.bind.annotation.GetMapping; // 5.8
import org.springframework.web.bind.annotation.PathVariable; // 5.9
import org.springframework.web.bind.annotation.RequestMapping; // 5.10
import org.springframework.web.bind.annotation.RequestParam; // 5.11
import org.springframework.web.bind.annotation.RestController; // 5.12

@RestController // 5.13
@RequestMapping("/api/v1") // 5.14 Prefix chung vì có 2 nhánh /cinemas/... và /showtimes/...
public class ShowtimeController { // 5.15

    private final ShowtimeService showtimeService; // 5.16

    public ShowtimeController(ShowtimeService showtimeService) { // 5.17
        this.showtimeService = showtimeService; // 5.18
    }

    @GetMapping("/cinemas/{cinemaId}/showtimes") // 5.19 GET /api/v1/cinemas/{id}/showtimes?date=...
    public List<ShowtimeResponse> listByCinema( // 5.20
            @PathVariable UUID cinemaId, // 5.21
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, // 5.22 Bắt buộc, dạng yyyy-MM-dd
            @RequestParam(required = false) UUID movieId) { // 5.23 Lọc theo phim (tùy chọn)
        return showtimeService.listByCinema(cinemaId, date, movieId); // 5.24
    }

    @GetMapping("/showtimes/{showtimeId}") // 5.25 GET /api/v1/showtimes/{id}
    public ShowtimeResponse get(@PathVariable UUID showtimeId) { // 5.26
        return showtimeService.get(showtimeId); // 5.27
    }
}