package com.cinema.payment.provider.vnpay;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.cinema.payment.provider.CheckoutRequest;
import com.cinema.payment.provider.CheckoutRules;
import com.cinema.payment.provider.CheckoutSession;
import com.cinema.payment.provider.PaymentProvider;
import com.cinema.payment.entity.Statuses;

@Component
public class VnpayPaymentProvider implements PaymentProvider {
    /** vnp_Amount (đã nhân 100) tối đa 12 chữ số. */
    static final long MAX_AMOUNT_VND = 9_999_999_999L;

    private final VnpayGateway gateway;
    private final VnpayProperties props;

    public VnpayPaymentProvider(VnpayGateway gateway, VnpayProperties props) {
        this.gateway = gateway;
        this.props = props;
    }

    @Override
    public String code() {
        return Statuses.PROVIDER_VNPAY;
    }

    @Override
    public CheckoutRules rules() {
        return new CheckoutRules(Set.of("VND"), MAX_AMOUNT_VND, props.getPaymentWindow(),
                props.getHoldSafetyMargin(), props.getMinHoldRemaining());
    }

    @Override
    public CheckoutSession createCheckout(CheckoutRequest request) {
        if (!"VND".equals(request.currency())) {
            throw new IllegalArgumentException("VNPay chỉ hỗ trợ VND");
        }

        String locale = request.options().get("locale");
        if (locale != null && !locale.equals("vn") && !locale.equals("en")) {
            throw new IllegalArgumentException("locale phải là vn hoặc en");
        }
        String url = gateway.buildCheckoutUrl(new VnpayGateway.CheckoutParams(
                request.merchantReference(), request.amountMinor(), request.description(), request.clientIp(),
                locale, request.options().get("bankCode"), request.createdAt(), request.expireAt()));
        return new CheckoutSession(url);
    }
}
