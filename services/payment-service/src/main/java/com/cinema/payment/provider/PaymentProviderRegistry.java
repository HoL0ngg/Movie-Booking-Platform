package com.cinema.payment.provider;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cinema.payment.exception.ApiException;

@Component
public class PaymentProviderRegistry {
    private final Map<String, PaymentProvider> byCode = new LinkedHashMap<>();
    private final String defaultCode;

    public PaymentProviderRegistry(List<PaymentProvider> providers,
            @Value("${payment.default-provider:vnpay}") String defaultCode) {
        for (PaymentProvider p : providers) {
            if (byCode.put(p.code(), p) != null) {
                throw new IllegalStateException("Trùng mã PaymentProvider: " + p.code());
            }
        }
        this.defaultCode = defaultCode.trim().toLowerCase();
        if (!byCode.containsKey(this.defaultCode)) {
            throw new IllegalStateException("payment.default-provider=" + this.defaultCode
                    + " không khớp provider nào đã đăng ký " + byCode.keySet());
        }
    }

    public PaymentProvider resolve(String code) {
        String key = code == null || code.isBlank() ? defaultCode : code.trim().toLowerCase();
        PaymentProvider provider = byCode.get(key);
        if (provider == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_PROVIDER",
                    "Phương thức thanh toán không được hỗ trợ: " + key);
        }

        return provider;
    }

    public Set<String> codes() {
        return Set.copyOf(byCode.keySet());
    }
}
