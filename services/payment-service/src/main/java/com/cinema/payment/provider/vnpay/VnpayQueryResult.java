package com.cinema.payment.provider.vnpay;

import com.cinema.payment.enums.ProviderOutcome;

/**
 * Kết quả API querydr. responseCode là mã xử lý của API (00 = truy vấn thành
 * công), KHÔNG phải kết quả thanh toán.
 */
public record VnpayQueryResult(
        boolean signatureValid,
        String responseCode,
        String message,
        String txnRef,
        long amount,
        String transactionNo,
        String transactionType,
        String transactionStatus,
        String payDate,
        String bankCode) {

    public boolean found() {
        return "00".equals(responseCode);
    }

    public boolean notFound() {
        return "91".equals(responseCode);
    }

    public ProviderOutcome outcome() {
        return VnpayCodec.outcomeOfStatus(transactionStatus);
    }

    public String eventIdentity() {
        return txnRef + ":" + transactionNo + ":" + transactionStatus;
    }
}
