package com.cinema.payment.webhook;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentReturnHandler {
    String provider();

    ParsedPaymentReturn parseReturn(HttpServletRequest request);
}
