package com.cinema.booking.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Payload của ShowtimePublished v1 (Cinema service). Thời gian là chuỗi
 * ISO-8601.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShowtimeSnapshotPayload(
        UUID showtimeId,
        UUID cinemaId,
        UUID auditoriumId,
        UUID movieId,
        String startsAt,
        String salesCloseAt,
        String currency,
        String status,
        long snapshotVersion,
        List<Seat> seats) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Seat(UUID seatId, String seatLabel, String seatType, long priceMinor) {
    }
}
