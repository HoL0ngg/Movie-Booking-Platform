package com.cinema.payment.messaging;

public final class EventTypes {
    private EventTypes() {
    }

    // Phát đi (topic cinema.payment-events.v1). Tên PaymentSucceeded/PaymentFailed
    // khớp booking-service.
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_REFUNDED = "PaymentRefunded";
    public static final String PAYMENT_REFUND_FAILED = "PaymentRefundFailed";

    // Nhận về (topic cinema.booking-events.v1)
    public static final String BOOKING_CONFIRMATION_FAILED = "BookingConfirmationFailed";
}
