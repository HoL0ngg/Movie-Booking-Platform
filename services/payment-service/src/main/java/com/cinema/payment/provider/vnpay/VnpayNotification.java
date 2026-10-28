package com.cinema.payment.provider.vnpay;

import java.util.Map;

import com.cinema.payment.enums.ProviderOutcome;

/**
 * Dữ liệu VNPay gửi về qua IPN hoặc Return URL, đã kiểm tra chữ ký.
 * amount là số tiền theo đơn vị VNPay (đã nhân 100); -1 nếu không parse được.
 */
public record VnpayNotification(
        boolean signatureValid,
        String tmnCode,
        String txnRef,
        long amount,
        String transactionNo,
        String responseCode,
        String transactionStatus,
        String payDate,
        String bankCode,
        Map<String, String> params) {

    /**
     * Thành công chỉ khi vnp_ResponseCode = 00 và vnp_TransactionStatus = 00 (hoặc
     * vắng mặt).
     * Trạng thái chưa chốt (01/04/07) -> PENDING; mọi trường hợp còn lại -> FAILED.
     */
    public ProviderOutcome outcome() {
        boolean statusOk = transactionStatus == null || "00".equals(transactionStatus);
        if ("00".equals(responseCode) && statusOk) {
            return ProviderOutcome.SUCCEEDED;
        }
        if (transactionStatus != null
                && VnpayCodec.outcomeOfStatus(transactionStatus) == ProviderOutcome.PENDING) {
            return ProviderOutcome.PENDING;
        }
        return ProviderOutcome.FAILED;
    }

    /** Định danh sự kiện để khử trùng lặp (IPN retry, trùng với querydr). */
    public String eventIdentity() {
        String status = transactionStatus != null ? transactionStatus : responseCode;
        return txnRef + ":" + transactionNo + ":" + status;
    }

    public String failureCode() {
        return responseCode != null ? responseCode : "UNKNOWN";
    }
}
