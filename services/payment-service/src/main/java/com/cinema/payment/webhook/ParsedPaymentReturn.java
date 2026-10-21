package com.cinema.payment.webhook;

public record ParsedPaymentReturn(
        boolean signatureValid,
        String transactionRef,
        String providerCode) {
}