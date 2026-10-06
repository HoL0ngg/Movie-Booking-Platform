package com.cinema.cinema.service; // 11.1

import java.time.Instant; // 11.2
import java.util.UUID; // 11.3

import org.slf4j.MDC; // 11.4
import org.springframework.stereotype.Service; // 11.5
import org.springframework.transaction.annotation.Propagation; // 11.6
import org.springframework.transaction.annotation.Transactional; // 11.7 Nơi TraceIdFilter lưu traceId của request
import org.springframework.util.StringUtils; // 11.8

import com.cinema.cinema.entity.OutboxEventEntity; // 11.9
import com.cinema.cinema.filter.TraceIdFilter; // 11.10
import com.cinema.cinema.repository.OutboxEventRepository; // 11.11

@Service // 11.12
public class OutboxService { // 11.13

    private final OutboxEventRepository outboxEventRepository; // 11.14

    public OutboxService(OutboxEventRepository outboxEventRepository) { // 11.15
        this.outboxEventRepository = outboxEventRepository; // 11.16
    }

    @Transactional(propagation = Propagation.MANDATORY) // 11.17 Bắt buộc gọi trong transaction có sẵn (outbox cùng transaction với thay đổi nghiệp vụ)
    public void record(String eventType, UUID aggregateId, String payloadJson, Instant now) { // 11.18
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_ATTRIBUTE); // 11.19 Lấy traceId của request hiện tại
        if (!StringUtils.hasText(traceId)) { // 11.20 Cột trace_id NOT NULL
            traceId = UUID.randomUUID().toString(); // 11.21
        }
        outboxEventRepository.save(new OutboxEventEntity(UUID.randomUUID(), eventType, 1, aggregateId, // 11.22 eventId mới, eventVersion = 1
                now, traceId, payloadJson, now)); // 11.23
    }
}