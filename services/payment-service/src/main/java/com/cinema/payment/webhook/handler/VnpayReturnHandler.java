package com.cinema.payment.webhook.handler;

import com.cinema.payment.provider.vnpay.VnpayGateway;
import com.cinema.payment.provider.vnpay.VnpayNotification;
import com.cinema.payment.webhook.ParsedPaymentReturn;
import com.cinema.payment.webhook.PaymentReturnHandler;
import com.cinema.payment.webhook.utils.ProviderRequestUtils;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class VnpayReturnHandler implements PaymentReturnHandler {
    private final VnpayGateway vnpayGateway;

    public VnpayReturnHandler(VnpayGateway vnpayGateway) {
        this.vnpayGateway = vnpayGateway;
    }

    @Override
    public String provider() {
        return "vnpay";
    }

    @Override
    public ParsedPaymentReturn parseReturn(HttpServletRequest request) {
        // Đọc tham số return theo định dạng VNPay.
        VnpayNotification n = vnpayGateway.parseNotification(
                ProviderRequestUtils.params(request));

        // Chuyển dữ liệu VNPay về cấu trúc chung của hệ thống.
        return new ParsedPaymentReturn(
                n.signatureValid(),
                n.txnRef(),
                n.responseCode());
    }
}
