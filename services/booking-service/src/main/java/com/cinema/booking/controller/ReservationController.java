package com.cinema.booking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.booking.dto.CreateReservationRequest;
import com.cinema.booking.dto.ReservationResponse;
import com.cinema.booking.service.ReservationService;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateReservationRequest request) {

        var body = reservationService.create(userId, idempotencyKey, request);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + body.reservationId())).body(body);
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID reservationId) {
        return reservationService.get(userId, reservationId);
    }

    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID reservationId) {
        reservationService.cancel(userId, reservationId);
    }

}
