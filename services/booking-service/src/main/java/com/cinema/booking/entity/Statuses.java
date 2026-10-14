package com.cinema.booking.entity;

public final class Statuses {
    private Statuses() {
    }

    // showtime_seats.status
    public static final String SEAT_AVAILABLE = "AVAILABLE";
    public static final String SEAT_HELD = "HELD";
    public static final String SEAT_BOOKED = "BOOKED";

    // reservations.status
    public static final String RES_HELD = "HELD";
    public static final String RES_CONFIRMED = "CONFIRMED";
    public static final String RES_CANCELLED = "CANCELLED";
    public static final String RES_EXPIRED = "EXPIRED";

    // bookings.status
    public static final String BOOKING_CONFIRMED = "CONFIRMED";

    // showtime_snapshots.status
    public static final String SHOWTIME_OPEN = "OPEN";
}
