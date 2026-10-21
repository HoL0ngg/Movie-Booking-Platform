package com.cinema.payment.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.payment.dto.CreatePaymentRequest;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.dto.RefundResponse;
import com.cinema.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest http) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var result = paymentService.create(userId, idempotencyKey, request, clientIp(http), jwt.getTokenValue());
        PaymentResponse body = result.payment();
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/payments/" + body.paymentId())).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return paymentService.get(UUID.fromString(jwt.getSubject()), paymentId);
    }

    @GetMapping("/{paymentId}/refunds")
    public List<RefundResponse> refunds(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return paymentService.refunds(UUID.fromString(jwt.getSubject()), paymentId);
    }

    /** IP khách cho vnp_IpAddr (chỉ mang tính thông tin với VNPay). */
    static String clientIp(HttpServletRequest request) {
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
