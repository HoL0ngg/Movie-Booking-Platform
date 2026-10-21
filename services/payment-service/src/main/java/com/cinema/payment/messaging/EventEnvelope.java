package com.cinema.payment.messaging;

import com.cinema.payment.common.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Định dạng chung của mọi event, cả gửi đi lẫn nhận về.
 * occurredAt là chuỗi ISO-8601 (Instant.toString()).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID aggregateId,
        String occurredAt,
        String traceId,
        JsonNode payload) {

    public static EventEnvelope parse(String raw) {
        EventEnvelope envelope;
        try {
            envelope = JsonUtils.fromJson(raw, EventEnvelope.class);
        } catch (IllegalStateException e) {
            throw new PoisonMessageException("Không parse được envelope", e);
        }
        if (envelope == null || envelope.eventId() == null || envelope.eventType() == null
                || envelope.payload() == null) {
            throw new PoisonMessageException("Envelope thiếu eventId/eventType/payload");
        }
        return envelope;
    }

    public <T> T payloadAs(Class<T> type) {
        try {
            return JsonUtils.treeToValue(payload, type);
        } catch (IllegalStateException e) {
            throw new PoisonMessageException(
                    "Payload của " + eventType + " không hợp lệ (" + eventId + ")", e);
        }
    }
}
