package com.cinema.payment.provider.vnpay;

/** Kết quả API refund. */
public record VnpayRefundResult(
        boolean signatureValid,
        String responseCode,
        String message,
        String transactionNo,
        String transactionStatus,
        long amount) {
}
