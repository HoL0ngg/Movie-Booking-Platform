package com.cinema.payment.webhook;

import com.cinema.payment.entity.ProviderResult;

/**
 * Kết quả phân tích return URL. {@code settlement} là kết quả đã chuẩn hóa theo cùng cách với IPN; chỉ dùng cho
 * chế độ dev {@code payment.vnpay.dev-settle-from-return}, mặc định không được dùng để kết luận thanh toán.
 */
public record ParsedPaymentReturn(
        boolean signatureValid,
        String transactionRef,
        String providerCode,
        ProviderResult settlement) {
}
