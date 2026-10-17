package com.cinema.booking.messaging;

public final class EventTypes {
    private EventTypes() {
    }

    // Phát đi (topic cinema.booking-events.v1)
    public static final String BOOKING_CONFIRMED = "BookingConfirmed";
    public static final String BOOKING_CONFIRMATION_FAILED = "BookingConfirmationFailed";
    public static final String RESERVATION_RELEASED = "ReservationReleased";
    public static final String RESERVATION_EXPIRED = "ReservationExpired";

    // Nhận về (topic cinema.payment-events.v1) - đặt tên theo giả định, chỉnh theo
    // hợp đồng thật
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";

    // Nhận về (topic cinema.showtime-events.v1)
    public static final String SHOWTIME_SNAPSHOT_UPSERTED = "ShowtimePublished";

}
