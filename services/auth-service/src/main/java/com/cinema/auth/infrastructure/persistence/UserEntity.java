package com.cinema.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "email", nullable = false, columnDefinition = "text")
    String email;

    @Column(name = "email_normalized", nullable = false, columnDefinition = "text")
    String emailNormalized;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    String passwordHash;

    @Column(name = "status", nullable = false, length = 16)
    String status;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public UserEntity() {}
}
