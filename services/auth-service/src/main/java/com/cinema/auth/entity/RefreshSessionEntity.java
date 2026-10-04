package com.cinema.auth.entity; // 12.85

import java.time.Instant; // 12.86
import java.util.UUID; // 12.87

import jakarta.persistence.Column; // 12.88
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // 12.89
@Table(name = "refresh_sessions") // 12.90
public class RefreshSessionEntity { // 12.91
    @Id // 12.92
    @Column(name = "id", nullable = false) // 12.93
    UUID id; // 12.94

    @Column(name = "user_id", nullable = false) // 12.95
    UUID userId; // 12.96

    @Column(name = "token_hash", nullable = false, columnDefinition = "bytea") // 12.97
    byte[] tokenHash; // 12.98

    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamptz") // 12.99
    Instant expiresAt; // 12.100

    @Column(name = "revoked_at", columnDefinition = "timestamptz") // 12.101 Cho phép null = chưa thu hồi
    Instant revokedAt; // 12.102

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz") // 12.103
    Instant createdAt; // 12.104

    public RefreshSessionEntity() {} // 12.105

    public RefreshSessionEntity(UUID id, UUID userId, byte[] tokenHash, Instant expiresAt, Instant createdAt) { // 12.106
        this.id = id; // 12.107
        this.userId = userId; // 12.108
        this.tokenHash = tokenHash; // 12.109
        this.expiresAt = expiresAt; // 12.110
        this.createdAt = createdAt; // 12.111
    }

    public UUID getUserId() { return userId; } // 12.112
    public Instant getExpiresAt() { return expiresAt; } // 12.113
    public Instant getRevokedAt() { return revokedAt; } // 12.114
    public void revoke(Instant now) { this.revokedAt = now; } // 12.115 Đánh dấu thu hồi
}