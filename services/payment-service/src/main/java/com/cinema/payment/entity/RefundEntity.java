package com.cinema.payment.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "refunds")
public class RefundEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "payment_id", nullable = false)
    UUID paymentId;

    @Column(name = "refund_request_id", nullable = false)
    UUID refundRequestId;

    @Column(name = "amount_minor", nullable = false)
    Long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Column(name = "request_key", nullable = false, length = 160)
    String requestKey;

    @Column(name = "request_hash", nullable = false, columnDefinition = "bytea")
    byte[] requestHash;

    @Column(name = "provider", nullable = false, length = 40)
    String provider;

    @Column(name = "merchant_reference", nullable = false, length = 160)
    String merchantReference;

    @Column(name = "provider_refund_id", length = 160)
    String providerRefundId;

    @Column(name = "status", nullable = false, length = 24)
    String status;

    @Column(name = "failure_code", length = 80)
    String failureCode;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public RefundEntity() {}
}
