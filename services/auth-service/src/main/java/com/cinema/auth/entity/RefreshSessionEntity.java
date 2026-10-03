package com.cinema.auth.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSessionEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "token_hash", nullable = false, columnDefinition = "bytea")
    byte[] tokenHash;

    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamptz")
    Instant expiresAt;

    @Column(name = "revoked_at", columnDefinition = "timestamptz")
    Instant revokedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    public RefreshSessionEntity() {}
}
