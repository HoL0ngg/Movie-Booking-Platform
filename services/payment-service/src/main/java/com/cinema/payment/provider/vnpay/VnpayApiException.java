package com.cinema.payment.provider.vnpay;

/**
 * Lỗi giao tiếp với API VNPay (timeout, HTTP != 200, JSON sai). Kết quả giao
 * dịch là CHƯA BIẾT.
 */
public class VnpayApiException extends RuntimeException {
    public VnpayApiException(String message) {
        super(message);
    }

    public VnpayApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
