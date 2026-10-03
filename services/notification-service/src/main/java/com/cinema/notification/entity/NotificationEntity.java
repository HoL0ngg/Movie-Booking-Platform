package com.cinema.notification.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "source_event_id", nullable = false)
    UUID sourceEventId;

    @Column(name = "event_type", nullable = false, length = 80)
    String eventType;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "channel", nullable = false, length = 16)
    String channel;

    @Column(name = "recipient_address", nullable = false, columnDefinition = "text")
    String recipientAddress;

    @Column(name = "status", nullable = false, length = 16)
    String status;

    @Column(name = "attempt_count", nullable = false)
    Integer attemptCount;

    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "timestamptz")
    Instant nextAttemptAt;

    @Column(name = "provider_message_id", length = 160)
    String providerMessageId;

    @Column(name = "last_error_code", length = 80)
    String lastErrorCode;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    @Column(name = "sent_at", columnDefinition = "timestamptz")
    Instant sentAt;

    public NotificationEntity() {}
}
