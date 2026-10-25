package com.cinema.payment.messaging;

public final class EventTypes {
    private EventTypes() {
    }

    // Phát đi (topic cinema.payment-events.v1). Tên PaymentSucceeded/PaymentFailed
    // khớp booking-service.
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String REFUND_COMPLETED = "RefundCompleted";
    public static final String REFUND_FAILED = "RefundFailed";

    // Nhận về (topic cinema.booking-events.v1)
    public static final String PAYMENT_REQUESTED = "PaymentRequested";
    public static final String BOOKING_CONFIRMATION_FAILED = "BookingConfirmationFailed";
}
