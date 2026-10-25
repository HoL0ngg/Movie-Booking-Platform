package com.cinema.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Yêu cầu tạo thanh toán. Số tiền KHÔNG do client gửi: payment-service lấy từ
 * reservation của booking-service.
 *
 * @param bankCode tuỳ chọn: VNPAYQR, VNBANK, INTCARD hoặc mã ngân hàng; bỏ
 *                 trống để khách chọn tại VNPay
 * @param locale   tuỳ chọn: vn | en
 * @param provider tuỳ chọn: mã phương thức thanh toán (vd. vnpay); bỏ trống
 */
public record CreatePaymentRequest(
                @NotNull UUID reservationId,
                @Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$", message = "provider không hợp lệ") String provider,
                @Pattern(regexp = "^[A-Za-z0-9]{3,20}$", message = "bankCode không hợp lệ") String bankCode,
                @Pattern(regexp = "^(vn|en)$", message = "locale phải là vn hoặc en") String locale) {
}
