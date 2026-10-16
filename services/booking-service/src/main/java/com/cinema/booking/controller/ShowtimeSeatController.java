package com.cinema.booking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.booking.dto.GetSeatsResponse;
import com.cinema.booking.service.ShowtimeSeatService;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeSeatController {
    private final ShowtimeSeatService showtimeSeatService;

    public ShowtimeSeatController(ShowtimeSeatService showtimeSeatService) {
        this.showtimeSeatService = showtimeSeatService;
    }

    @GetMapping("/{showtimeId}/seats")
    public List<GetSeatsResponse> getSeats(@PathVariable UUID showtimeId) {
        return showtimeSeatService.getSeats(showtimeId);
    }

}
