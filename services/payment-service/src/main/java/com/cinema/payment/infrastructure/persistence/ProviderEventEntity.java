package com.cinema.payment.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "provider_events")
public class ProviderEventEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "payment_id", nullable = false)
    UUID paymentId;

    @Column(name = "provider", nullable = false, length = 40)
    String provider;

    @Column(name = "event_identity", nullable = false, length = 160)
    String eventIdentity;

    @Column(name = "provider_event_id", length = 160)
    String providerEventId;

    @Column(name = "payload_digest", columnDefinition = "bytea")
    byte[] payloadDigest;

    @Column(name = "result_code", nullable = false, length = 80)
    String resultCode;

    @Column(name = "received_at", nullable = false, columnDefinition = "timestamptz")
    Instant receivedAt;

    public ProviderEventEntity() {}
}
