package com.cinema.payment.webhook;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentWebhookHandler {
    String provider();

    ParsedWebhook parseNotification(HttpServletRequest request);
}
