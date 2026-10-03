package com.cinema.payment.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "booking_id", nullable = false)
    UUID bookingId;

    @Column(name = "reservation_id", nullable = false)
    UUID reservationId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "amount_minor", nullable = false)
    Long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Column(name = "request_key", nullable = false, length = 160)
    String requestKey;

    @Column(name = "request_hash", nullable = false, columnDefinition = "bytea")
    byte[] requestHash;

    @Column(name = "status", nullable = false, length = 24)
    String status;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public PaymentEntity() {}
}
