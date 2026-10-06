package com.cinema.cinema.messaging; // 12.1

import java.time.Instant; // 12.2
import java.util.List; // 12.3
import java.util.UUID; // 12.4
import java.util.stream.Collectors; // 12.5

import com.cinema.cinema.entity.OutboxEventEntity; // 12.6
import com.cinema.cinema.entity.SeatEntity; // 12.7
import com.cinema.cinema.entity.ShowtimeEntity; // 12.8

public final class EventJson { // 12.9 Tiện ích dựng JSON thủ công theo event-envelope.schema.json

    private EventJson() { // 12.10 Không cho tạo instance
    }

    public static String quote(String value) { // 12.11 Chuỗi → literal JSON có escape
        StringBuilder sb = new StringBuilder("\""); // 12.12
        for (char c : value.toCharArray()) { // 12.13
            switch (c) { // 12.14
                case '"' -> sb.append("\\\""); // 12.15
                case '\\' -> sb.append("\\\\"); // 12.16
                case '\n' -> sb.append("\\n"); // 12.17
                case '\r' -> sb.append("\\r"); // 12.18
                case '\t' -> sb.append("\\t"); // 12.19
                default -> { // 12.20
                    if (c < 0x20) { // 12.21 Ký tự điều khiển
                        sb.append(String.format("\\u%04x", (int) c)); // 12.22
                    } else { // 12.23
                        sb.append(c); // 12.24
                    }
                }
            }
        }
        return sb.append('"').toString(); // 12.25
    }

    public static String publishedPayload(ShowtimeEntity s, UUID cinemaId, List<SeatEntity> seats) { // 12.26 Payload ShowtimePublished
        String seatsJson = seats.stream() // 12.27 Thứ tự ghế đã chuẩn hóa từ query
                .map(seat -> "{\"seatId\":" + quote(seat.getId().toString()) // 12.28
                        + ",\"label\":" + quote(seat.getRowLabel() + seat.getSeatNumber()) // 12.29 Nhãn ghế, vd "A5"
                        + ",\"type\":" + quote(seat.getSeatType()) // 12.30
                        + ",\"priceMinor\":" + s.getPriceMinor() + "}") // 12.31 Giá của suất chiếu
                .collect(Collectors.joining(",")); // 12.32
        return "{\"showtimeId\":" + quote(s.getId().toString()) // 12.33 Đủ và chỉ đúng các field schema yêu cầu
                + ",\"cinemaId\":" + quote(cinemaId.toString()) // 12.34
                + ",\"auditoriumId\":" + quote(s.getAuditoriumId().toString()) // 12.35
                + ",\"movieId\":" + quote(s.getMovieId().toString()) // 12.36
                + ",\"startsAt\":" + quote(s.getStartsAt().toString()) // 12.37
                + ",\"salesCloseAt\":" + quote(s.getSalesCloseAt().toString()) // 12.38
                + ",\"currency\":" + quote(s.getCurrency()) // 12.39
                + ",\"seats\":[" + seatsJson + "]" // 12.40
                + ",\"snapshotVersion\":" + s.getSnapshotVersion() + "}"; // 12.41
    }

    public static String cancelledPayload(UUID showtimeId, long snapshotVersion, String reasonCode, Instant cancelledAt) { // 12.42 Payload ShowtimeCancelled
        return "{\"showtimeId\":" + quote(showtimeId.toString()) // 12.43
                + ",\"snapshotVersion\":" + snapshotVersion // 12.44
                + ",\"reasonCode\":" + quote(reasonCode) // 12.45
                + ",\"cancelledAt\":" + quote(cancelledAt.toString()) + "}"; // 12.46
    }

    public static String envelope(OutboxEventEntity e) { // 12.47 Bọc payload bằng envelope chuẩn
        return "{\"eventId\":" + quote(e.getEventId().toString()) // 12.48 eventId giữ nguyên mỗi lần gửi lại
                + ",\"eventType\":" + quote(e.getEventType()) // 12.49
                + ",\"eventVersion\":" + e.getEventVersion() // 12.50
                + ",\"aggregateId\":" + quote(e.getAggregateId().toString()) // 12.51
                + ",\"occurredAt\":" + quote(e.getOccurredAt().toString()) // 12.52
                + ",\"traceId\":" + quote(e.getTraceId()) // 12.53
                + ",\"payload\":" + e.getPayload() + "}"; // 12.54 payload đã là JSON, nhúng nguyên
    }
}