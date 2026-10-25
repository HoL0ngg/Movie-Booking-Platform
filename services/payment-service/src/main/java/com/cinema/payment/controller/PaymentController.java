package com.cinema.payment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.dto.RefundResponse;
import com.cinema.payment.service.PaymentService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return paymentService.get(UUID.fromString(jwt.getSubject()), paymentId);
    }

    @GetMapping("/{paymentId}/refunds")
    public List<RefundResponse> refunds(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return paymentService.refunds(UUID.fromString(jwt.getSubject()), paymentId);
    }
}
