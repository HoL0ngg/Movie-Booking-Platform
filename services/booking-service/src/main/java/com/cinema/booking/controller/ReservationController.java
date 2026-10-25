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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateReservationRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        var body = reservationService.create(userId, idempotencyKey, request);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + body.reservationId())).body(body);
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reservationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return reservationService.get(userId, reservationId);
    }

    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reservationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        reservationService.cancel(userId, reservationId);
    }

}
