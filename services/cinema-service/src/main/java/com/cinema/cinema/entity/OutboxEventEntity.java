package com.cinema.cinema.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    @Column(name = "event_id", nullable = false)
    UUID eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    String eventType;

    @Column(name = "event_version", nullable = false)
    Integer eventVersion;

    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "timestamptz")
    Instant occurredAt;

    @Column(name = "trace_id", nullable = false, length = 128)
    String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    String payload;

    @Column(name = "publish_attempts", nullable = false)
    Integer publishAttempts;

    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "timestamptz")
    Instant nextAttemptAt;

    @Column(name = "published_at", columnDefinition = "timestamptz")
    Instant publishedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    public OutboxEventEntity() {}

    public OutboxEventEntity(UUID eventId, String eventType, Integer eventVersion, UUID aggregateId, // 15.44 Constructor tạo event mới
                             Instant occurredAt, String traceId, String payload, Instant now) { // 15.45
        this.eventId = eventId; // 15.46
        this.eventType = eventType; // 15.47
        this.eventVersion = eventVersion; // 15.48
        this.aggregateId = aggregateId; // 15.49
        this.occurredAt = occurredAt; // 15.50
        this.traceId = traceId; // 15.51
        this.payload = payload; // 15.52 JSON dạng chuỗi
        this.publishAttempts = 0; // 15.53
        this.nextAttemptAt = now; // 15.54 Đến hạn gửi ngay
        this.createdAt = now; // 15.55
    }

    public void markPublished(Instant now) { // 15.56
        this.publishedAt = now; // 15.57 Đánh dấu đã gửi (relay sẽ không lấy lại)
    }

    public void markFailed(Instant now) { // 15.58
        this.publishAttempts = this.publishAttempts + 1; // 15.59
        long delaySeconds = Math.min(1L << Math.min(this.publishAttempts, 6), 60); // 15.60 Backoff lũy thừa 2, tối đa 60 giây
        this.nextAttemptAt = now.plusSeconds(delaySeconds); // 15.61
    }

    public UUID getEventId() { return eventId; } // 15.62
    public String getEventType() { return eventType; } // 15.63
    public Integer getEventVersion() { return eventVersion; } // 15.64
    public UUID getAggregateId() { return aggregateId; } // 15.65
    public Instant getOccurredAt() { return occurredAt; } // 15.66
    public String getTraceId() { return traceId; } // 15.67
    public String getPayload() { return payload; } // 15.68
    public Integer getPublishAttempts() { return publishAttempts; } // 15.69
}
