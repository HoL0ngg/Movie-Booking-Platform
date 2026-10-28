package com.cinema.booking.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.booking.common.JsonUtils;
import com.cinema.booking.entity.OutboxEventEntity;
import com.cinema.booking.repository.OutboxEventRepository;

@Service
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;

    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, int eventVersion, UUID aggregateId, Object payload) {
        Instant now = Instant.now();
        String traceId = MDC.get("traceId"); // TraceIdFilter (HTTP) hoặc consumer set sẵn
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString(); // trace_id NOT NULL
        }

        var event = new OutboxEventEntity();
        event.setEventId(UUID.randomUUID());
        event.setEventType(eventType);
        event.setEventVersion(eventVersion);
        event.setAggregateId(aggregateId);
        event.setOccurredAt(now);
        event.setTraceId(traceId);
        event.setPayload(JsonUtils.toJson(payload));
        event.setPublishAttempts(0);
        event.setNextAttemptAt(now);
        event.setCreatedAt(now);
        outboxEventRepository.save(event);
    }
}
