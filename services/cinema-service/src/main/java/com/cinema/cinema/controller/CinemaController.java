package com.cinema.cinema.controller; // 4.1

import java.util.List; // 4.2
import java.util.UUID; // 4.3

import org.springframework.web.bind.annotation.GetMapping; // 4.4
import org.springframework.web.bind.annotation.PathVariable; // 4.5
import org.springframework.web.bind.annotation.RequestMapping; // 4.6
import org.springframework.web.bind.annotation.RequestParam; // 4.7
import org.springframework.web.bind.annotation.RestController; // 4.8

import com.cinema.cinema.dto.CinemaDetailResponse; // 4.9
import com.cinema.cinema.dto.CinemaResponse; // 4.10
import com.cinema.cinema.service.CinemaService; // 4.11

@RestController // 4.12
@RequestMapping("/api/v1/cinemas") // 4.13
public class CinemaController { // 4.14

    private final CinemaService cinemaService; // 4.15

    public CinemaController(CinemaService cinemaService) { // 4.16 Constructor injection
        this.cinemaService = cinemaService; // 4.17
    }

    @GetMapping // 4.18 GET /api/v1/cinemas
    public List<CinemaResponse> list(@RequestParam(required = false) String city) { // 4.19 ?city= (tùy chọn)
        return cinemaService.list(city); // 4.20
    }

    @GetMapping("/{cinemaId}") // 4.21 GET /api/v1/cinemas/{cinemaId}
    public CinemaDetailResponse get(@PathVariable UUID cinemaId) { // 4.22
        return cinemaService.get(cinemaId); // 4.23
    }
}