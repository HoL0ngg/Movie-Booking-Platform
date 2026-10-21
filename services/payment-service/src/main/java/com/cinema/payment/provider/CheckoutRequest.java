package com.cinema.payment.provider;

import java.time.Instant;
import java.util.Map;

/**
 * Yêu cầu tạo checkout, độc lập với provider.
 *
 * @param merchantReference mã tham chiếu duy nhất của lần thử thanh toán (do
 *                          PaymentService sinh)
 * @param amountMinor       số tiền theo đơn vị nhỏ nhất của {@code currency}
 *                          (VND: đồng)
 * @param description       mô tả đơn hàng, ASCII không dấu để mọi cổng đều chấp
 *                          nhận
 * @param clientIp          IP của khách
 * @param createdAt         thời điểm tạo, đã làm tròn xuống giây
 * @param expireAt          hạn chót khách được thanh toán
 * @param options           tuỳ chọn riêng của provider (vd. "bankCode",
 *                          "locale"); provider bỏ qua khoá không biết
 */
public record CheckoutRequest(
        String merchantReference,
        long amountMinor,
        String currency,
        String description,
        String clientIp,
        Instant createdAt,
        Instant expireAt,
        Map<String, String> options) {

    public CheckoutRequest {
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
