package com.cinema.payment.provider;

import java.time.Duration;
import java.util.Set;

/**
 * Ràng buộc của một provider.
 *
 * @param supportedCurrencies mã tiền tệ được hỗ trợ (ISO 4217, chữ hoa)
 * @param maxAmountMinor      số tiền tối đa cho một giao dịch (đơn vị nhỏ nhất)
 * @param paymentWindow       thời gian tối đa khách được thanh toán kể từ lúc
 *                            tạo checkout
 * @param holdSafetyMargin    checkout hết hạn sớm hơn hold của reservation một
 *                            khoảng này
 * @param minHoldRemaining    từ chối tạo thanh toán nếu hold còn ít hơn khoảng
 *                            này
 */
public record CheckoutRules(
        Set<String> supportedCurrencies,
        long maxAmountMinor,
        Duration paymentWindow,
        Duration holdSafetyMargin,
        Duration minHoldRemaining) {

    public boolean supportsCurrency(String currency) {
        return supportedCurrencies.contains(currency);
    }
}
