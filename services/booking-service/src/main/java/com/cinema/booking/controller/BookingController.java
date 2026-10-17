package com.cinema.booking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.booking.dto.BookingResponse;
import com.cinema.booking.dto.ConfirmBookingRequest;
import com.cinema.booking.service.BookingService;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/reservations/{reservationId}/bookings")
    public ResponseEntity<BookingResponse> confirm(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID reservationId,
            @RequestBody ConfirmBookingRequest request) {
        var body = bookingService.confirm(userId, reservationId, request.paymentId());
        return ResponseEntity.created(URI.create("/api/v1/bookings/" + body.bookingId())).body(body);
    }

    @GetMapping("/bookings/{bookingId}")
    public BookingResponse get(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID bookingId) {
        return bookingService.get(userId, bookingId);
    }

}
