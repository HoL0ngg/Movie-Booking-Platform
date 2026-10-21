package com.cinema.payment.webhook;

import com.cinema.payment.entity.ProviderResult;

public record ParsedWebhook(
        boolean signatureValid,
        String transactionRef,
        ProviderResult result) {
}