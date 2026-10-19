package com.cinema.booking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.booking.dto.BookingResponse;
import com.cinema.booking.dto.ConfirmBookingRequest;
import com.cinema.booking.service.BookingService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/reservations/{reservationId}/bookings")
    public ResponseEntity<BookingResponse> confirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reservationId,
            @RequestBody ConfirmBookingRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var body = bookingService.confirm(userId, reservationId, request.paymentId());
        return ResponseEntity.created(URI.create("/api/v1/bookings/" + body.bookingId())).body(body);
    }

    @GetMapping("/bookings/{bookingId}")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return bookingService.get(userId, bookingId);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return bookingService.list(userId, limit);
    }

}
