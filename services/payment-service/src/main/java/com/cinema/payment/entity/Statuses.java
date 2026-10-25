package com.cinema.payment.entity;

/** Giá trị trạng thái, khớp các CHECK constraint trong db/schema.sql. */
public final class Statuses {
    private Statuses() {
    }

    public static final String PROVIDER_VNPAY = "vnpay";

    // payments.status
    public static final String PAY_REQUESTED = "REQUESTED";
    public static final String PAY_PROVIDER_PENDING = "PROVIDER_PENDING";
    public static final String PAY_SUCCEEDED = "SUCCEEDED";
    public static final String PAY_FAILED = "FAILED";
    public static final String PAY_REFUND_PENDING = "REFUND_PENDING";
    public static final String PAY_REFUNDED = "REFUNDED";
    public static final String PAY_REFUND_FAILED = "REFUND_FAILED";

    // payment_attempts.status
    public static final String ATTEMPT_REQUESTED = "REQUESTED";
    public static final String ATTEMPT_PROVIDER_PENDING = "PROVIDER_PENDING";
    public static final String ATTEMPT_SUCCEEDED = "SUCCEEDED";
    public static final String ATTEMPT_FAILED = "FAILED";
    public static final String ATTEMPT_UNKNOWN = "UNKNOWN";

    // refunds.status
    public static final String REFUND_REQUESTED = "REQUESTED";
    public static final String REFUND_PROVIDER_PENDING = "PROVIDER_PENDING";
    public static final String REFUND_COMPLETED = "COMPLETED";
    public static final String REFUND_FAILED = "FAILED";
}
