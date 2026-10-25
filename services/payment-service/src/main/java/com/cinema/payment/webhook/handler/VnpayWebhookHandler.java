package com.cinema.payment.webhook.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cinema.payment.entity.ProviderResult;
import com.cinema.payment.provider.vnpay.VnpayGateway;
import com.cinema.payment.provider.vnpay.VnpayNotification;
import com.cinema.payment.webhook.ParsedWebhook;
import com.cinema.payment.webhook.PaymentWebhookHandler;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class VnpayWebhookHandler implements PaymentWebhookHandler {
    private final VnpayGateway vnpayGateway;

    public VnpayWebhookHandler(VnpayGateway vnpayGateway) {
        this.vnpayGateway = vnpayGateway;
    }

    @Override
    public String provider() {
        return "vnpay";
    }

    @Override
    public ParsedWebhook parseNotification(HttpServletRequest request) {
        VnpayNotification notification = vnpayGateway.parseNotification(params(request));

        // Chuẩn hóa dữ liệu riêng của VNPay thành ProviderResult.
        ProviderResult result = new ProviderResult(
                notification.txnRef(),
                notification.amount(),
                notification.outcome(),
                notification.transactionNo(),
                notification.failureCode(),
                notification.eventIdentity());
        return new ParsedWebhook(
                notification.signatureValid(),
                notification.txnRef(),
                result);
    }

    private static Map<String, String> params(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        return params;
    }
}
