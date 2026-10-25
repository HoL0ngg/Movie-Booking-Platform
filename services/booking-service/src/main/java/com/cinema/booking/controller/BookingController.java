package com.cinema.booking.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.booking.dto.BookingResponse;
import com.cinema.booking.service.BookingService;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/reservations/{reservationId}/checkout")
    public ResponseEntity<BookingResponse> beginCheckout(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reservationId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var body = bookingService.beginCheckout(userId, reservationId, idempotencyKey, clientIp(request));
        return ResponseEntity.accepted().body(body);
    }

    @GetMapping("/bookings/{bookingId}")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookingId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return bookingService.get(userId, bookingId);
    }

    @GetMapping("/bookings/by-payment/{paymentId}")
    public BookingResponse getByPaymentId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return bookingService.getByPaymentId(userId, paymentId);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return bookingService.list(userId, limit);
    }

    // Xác định và chuẩn hóa IPClient, phục vụ cho tạo yêu cầu thanh toán VNPay
    // (trong payment_attempt.checkout_url)
    private static String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        if (ip == null || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "127.0.0.1";
        }
        return ip.length() < 7 || ip.length() > 45 ? "127.0.0.1" : ip;
    }
}
