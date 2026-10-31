package com.cinema.payment.webhook.handler;

import com.cinema.payment.entity.ProviderResult;
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
        // settlement giống hệt dữ liệu IPN (cùng eventIdentity nên idempotent với IPN/querydr đến sau);
        // controller chỉ dùng khi bật cờ dev-settle-from-return.
        ProviderResult settlement = new ProviderResult(
                n.txnRef(),
                n.amount(),
                n.outcome(),
                n.transactionNo(),
                n.failureCode(),
                n.eventIdentity());
        return new ParsedPaymentReturn(
                n.signatureValid(),
                n.txnRef(),
                n.responseCode(),
                settlement);
    }
}
