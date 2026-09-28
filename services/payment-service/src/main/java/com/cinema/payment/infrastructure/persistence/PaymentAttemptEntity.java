package com.cinema.payment.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "payment_id", nullable = false)
    UUID paymentId;

    @Column(name = "provider", nullable = false, length = 40)
    String provider;

    @Column(name = "merchant_reference", nullable = false, length = 160)
    String merchantReference;

    @Column(name = "provider_transaction_id", length = 160)
    String providerTransactionId;

    @Column(name = "checkout_url", columnDefinition = "text")
    String checkoutUrl;

    @Column(name = "status", nullable = false, length = 24)
    String status;

    @Column(name = "failure_code", length = 80)
    String failureCode;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public PaymentAttemptEntity() {}
}
