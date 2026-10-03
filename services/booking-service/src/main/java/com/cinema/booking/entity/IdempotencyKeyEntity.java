package com.cinema.booking.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyKeyEntity.Key.class)
public class IdempotencyKeyEntity {
    @Id
    @Column(name = "actor_id", nullable = false)
    UUID actorId;

    @Id
    @Column(name = "operation", nullable = false, length = 32)
    String operation;

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 160)
    String idempotencyKey;

    @Column(name = "request_hash", nullable = false, columnDefinition = "bytea")
    byte[] requestHash;

    @Column(name = "response_status")
    Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    String responseBody;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamptz")
    Instant expiresAt;

    public IdempotencyKeyEntity() {}

    public static class Key implements Serializable {
        public UUID actorId;
        public String operation;
        public String idempotencyKey;
        public Key() {}
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(actorId, key.actorId) && Objects.equals(operation, key.operation) && Objects.equals(idempotencyKey, key.idempotencyKey);
        }
        @Override public int hashCode() { return Objects.hash(actorId, operation, idempotencyKey); }
    }
}
