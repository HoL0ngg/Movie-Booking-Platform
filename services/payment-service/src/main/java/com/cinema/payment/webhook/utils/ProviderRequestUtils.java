package com.cinema.payment.webhook.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public final class ProviderRequestUtils {
    private ProviderRequestUtils() {
    }

    public static Map<String, String> params(
            HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();

        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        return params;
    }
}
