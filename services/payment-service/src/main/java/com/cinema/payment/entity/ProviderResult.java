package com.cinema.payment.entity;

import com.cinema.payment.enums.ProviderOutcome;

public record ProviderResult(
        String txnRef,
        Long amountVnp, // số tiền theo đơn vị VNPay (đã nhân 100); null = không kiểm tra (vd. hết hạn
                        // do hệ thống)
        ProviderOutcome outcome,
        String transactionNo,
        String failureCode,
        String eventIdentity) {
}