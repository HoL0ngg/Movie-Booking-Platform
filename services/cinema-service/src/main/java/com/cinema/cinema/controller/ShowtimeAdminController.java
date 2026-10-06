package com.cinema.cinema.controller; // 6.1

import java.util.UUID; // 6.2

import org.springframework.http.HttpStatus; // 6.3
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 6.4
import org.springframework.security.oauth2.jwt.Jwt; // 6.5
import org.springframework.web.bind.annotation.PathVariable; // 6.6
import org.springframework.web.bind.annotation.PostMapping; // 6.7
import org.springframework.web.bind.annotation.RequestBody; // 6.8
import org.springframework.web.bind.annotation.RequestMapping; // 6.9
import org.springframework.web.bind.annotation.ResponseStatus; // 6.10
import org.springframework.web.bind.annotation.RestController; // 6.11

import com.cinema.cinema.dto.CancelShowtimeRequest; // 6.12
import com.cinema.cinema.dto.CreateShowtimeRequest; // 6.13
import com.cinema.cinema.dto.ShowtimeResponse; // 6.14
import com.cinema.cinema.service.ShowtimeService; // 6.15

import jakarta.validation.Valid; // 6.16

@RestController // 6.17
@RequestMapping("/api/v1/showtimes") // 6.18
public class ShowtimeAdminController { // 6.19

    private final ShowtimeService showtimeService; // 6.20

    public ShowtimeAdminController(ShowtimeService showtimeService) { // 6.21
        this.showtimeService = showtimeService; // 6.22
    }

    @PostMapping // 6.23 POST /api/v1/showtimes (tạo DRAFT)
    @ResponseStatus(HttpStatus.CREATED) // 6.24 Trả 201
    public ShowtimeResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateShowtimeRequest request) { // 6.25 jwt = token đã verify
        return showtimeService.create(jwt, request); // 6.26
    }

    @PostMapping("/{showtimeId}/publish") // 6.27 DRAFT → PUBLISHED + ghi outbox
    public ShowtimeResponse publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID showtimeId) { // 6.28
        return showtimeService.publish(jwt, showtimeId); // 6.29
    }

    @PostMapping("/{showtimeId}/cancel") // 6.30 → CANCELLED (+ outbox nếu đã publish)
    public ShowtimeResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID showtimeId, // 6.31
                                   @Valid @RequestBody CancelShowtimeRequest request) { // 6.32
        return showtimeService.cancel(jwt, showtimeId, request); // 6.33
    }
}