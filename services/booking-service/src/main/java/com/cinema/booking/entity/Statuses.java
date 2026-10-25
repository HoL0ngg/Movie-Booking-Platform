package com.cinema.booking.entity;

public final class Statuses {
    private Statuses() {
    }

    // showtime_seats.status
    public static final String SEAT_AVAILABLE = "AVAILABLE";
    public static final String SEAT_HELD = "HELD";
    public static final String SEAT_PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String SEAT_BOOKED = "SOLD";

    // reservations.status
    public static final String RES_HELD = "HELD";
    public static final String RES_PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String RES_CONFIRMED = "CONFIRMED";
    public static final String RES_RELEASED = "RELEASED";
    public static final String RES_EXPIRED = "EXPIRED";

    // bookings.status
    public static final String BOOKING_PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String BOOKING_CONFIRMED = "CONFIRMED";
    public static final String BOOKING_CANCELLED = "CANCELLED";

    // showtime_snapshots.status
    public static final String SHOWTIME_OPEN = "PUBLISHED";
}
