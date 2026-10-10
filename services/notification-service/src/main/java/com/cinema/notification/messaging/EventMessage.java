package com.cinema.notification.messaging; // 4.5

import java.util.List; // 4.6
import java.util.Map; // 4.7
import java.util.UUID; // 4.8

import org.springframework.boot.json.JsonParser; // 4.9
import org.springframework.boot.json.JsonParserFactory; // 4.10 Bộ đọc JSON của Spring Boot

import com.cinema.notification.exception.InvalidEventException; // 4.11

public record EventMessage(UUID eventId, String eventType, Map<String, Object> payload) { // 4.12 Event đã đọc: id, loại, payload

    private static final JsonParser PARSER = JsonParserFactory.getJsonParser(); // 4.13 Tự chọn thư viện JSON có sẵn

    public static EventMessage parse(String json) { // 4.14 JSON envelope → EventMessage
        Map<String, Object> envelope; // 4.15
        try { // 4.16   
            envelope = PARSER.parseMap(json); // 4.17 JSON object → Map
        } catch (RuntimeException ex) { // 4.18 JSON hỏng hoặc null
            throw new InvalidEventException("Event is not valid JSON"); // 4.19
        }
        if (!(envelope.get("payload") instanceof Map<?, ?> rawPayload)) { // 4.20 payload phải là object
            throw new InvalidEventException("Event payload is missing"); // 4.21
        }
        return new EventMessage(toUuid(envelope.get("eventId"), "eventId"), // 4.22
                toText(envelope.get("eventType"), "eventType"), castMap(rawPayload)); // 4.23
    }

    public String text(String key) { return toText(payload.get(key), key); } // 4.24 Lấy field chuỗi bắt buộc trong payload
    public UUID uuid(String key) { return toUuid(payload.get(key), key); } // 4.25 Lấy field UUID bắt buộc

    public String number(String key) { // 4.26 Lấy field số nguyên (vd amountMinor) dưới dạng chuỗi
        if (payload.get(key) instanceof Number n) { // 4.27
            return String.valueOf(n.longValue()); // 4.28
        }
        throw new InvalidEventException("Missing number: " + key); // 4.29
    }

    public int listSize(String key) { // 4.30 Số phần tử của mảng (vd seatIds)
        if (payload.get(key) instanceof List<?> list) { // 4.31
            return list.size(); // 4.32
        }
        throw new InvalidEventException("Missing list: " + key); // 4.33
    }

    private static String toText(Object value, String key) { // 4.34
        if (value instanceof String s && !s.isBlank()) { // 4.35
            return s; // 4.36
        }
        throw new InvalidEventException("Missing text: " + key); // 4.37
    }

    private static UUID toUuid(Object value, String key) { // 4.38
        try { // 4.39
            return UUID.fromString(toText(value, key)); // 4.40
        } catch (IllegalArgumentException ex) { // 4.41 Không đúng định dạng UUID
            throw new InvalidEventException("Invalid UUID: " + key); // 4.42
        }
    }

    @SuppressWarnings("unchecked") // 4.43 Ép kiểu Map<?,?> → Map<String,Object>, JSON object luôn có khóa chuỗi
    private static Map<String, Object> castMap(Map<?, ?> map) { // 4.44
        return (Map<String, Object>) map; // 4.45
    }
}