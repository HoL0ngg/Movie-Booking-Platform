package com.cinema.cinema.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.cinema.dto.CreateShowtimeRequest;
import com.cinema.cinema.dto.ShowtimeResponse;
import com.cinema.cinema.service.ShowtimeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/showtimes")
public class AdminShowtimeController {
    private final ShowtimeService showtimes;

    public AdminShowtimeController(ShowtimeService showtimes) {
        this.showtimes = showtimes;
    }

    @GetMapping
    public List<ShowtimeResponse> list(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(required = false) UUID cinemaId) {
        return showtimes.listForAdmin(jwt, cinemaId);
    }

    @PutMapping("/{showtimeId}")
    public ShowtimeResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID showtimeId,
                                   @Valid @RequestBody CreateShowtimeRequest request) {
        return showtimes.updateDraft(jwt, showtimeId, request);
    }
}
