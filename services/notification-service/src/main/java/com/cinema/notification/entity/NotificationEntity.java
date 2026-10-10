package com.cinema.notification.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "subject", nullable = false, columnDefinition = "text") // 8.1 Ánh xạ cột mới (xem 2.2)
    String subject; // 8.2

    @Column(name = "body", nullable = false, columnDefinition = "text") // 8.3
    String body; // 8.4

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

        public NotificationEntity(UUID id, UUID sourceEventId, String eventType, UUID userId, String channel, // 8.5 Constructor tạo thông báo mới
                              String recipientAddress, String subject, String body, Instant now) { // 8.6
        this.id = id; // 8.7
        this.sourceEventId = sourceEventId; // 8.8
        this.eventType = eventType; // 8.9
        this.userId = userId; // 8.10
        this.channel = channel; // 8.11
        this.recipientAddress = recipientAddress; // 8.12
        this.subject = subject; // 8.13
        this.body = body; // 8.14
        this.status = "PENDING"; // 8.15 Chờ gửi
        this.attemptCount = 0; // 8.16
        this.nextAttemptAt = now; // 8.17 Đến hạn gửi ngay
        this.createdAt = now; // 8.18
        this.updatedAt = now; // 8.19
    }

    public void markSent(Instant now, String providerMessageId) { // 8.20 Gửi thành công
        this.status = "SENT"; // 8.21
        this.attemptCount = this.attemptCount + 1; // 8.22
        this.sentAt = now; // 8.23 Bắt buộc có khi SENT (CHECK ck_notifications_sent)
        this.providerMessageId = providerMessageId; // 8.24
        this.lastErrorCode = null; // 8.25
        this.updatedAt = now; // 8.26
    }

    public void markFailed(Instant now, String errorCode) { // 8.27 Gửi thất bại, lên lịch thử lại
        this.status = "FAILED"; // 8.28
        this.attemptCount = this.attemptCount + 1; // 8.29
        this.lastErrorCode = errorCode.length() > 80 ? errorCode.substring(0, 80) : errorCode; // 8.30 Cột chỉ dài 80
        this.nextAttemptAt = now.plusSeconds(Math.min(10L * (1L << Math.min(this.attemptCount, 5)), 600L)); // 8.31 Backoff lũy thừa 2, tối đa 10 phút
        this.updatedAt = now; // 8.32
    }

    public UUID getId() { return id; } // 8.33
    public String getRecipientAddress() { return recipientAddress; } // 8.34
    public String getSubject() { return subject; } // 8.35
    public String getBody() { return body; } // 8.36
    public Integer getAttemptCount() { return attemptCount; } // 8.37
}
