package com.cinema.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "roles")
public class RoleEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "code", nullable = false, length = 40)
    String code;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    public RoleEntity() {}
}
