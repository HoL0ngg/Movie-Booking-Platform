package com.cinema.cinema.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
}
