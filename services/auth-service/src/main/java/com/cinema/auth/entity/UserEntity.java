package com.cinema.auth.entity; // 12.1

import java.time.Instant; // 12.2 @Entity, @Table, @Id, @Column
import java.util.UUID; // 12.3

import jakarta.persistence.Column; // 12.4
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // 12.5 Ánh xạ class ↔ bảng
@Table(name = "users") // 12.6 Tên bảng
public class UserEntity { // 12.7
    @Id // 12.8 Khóa chính
    @Column(name = "id", nullable = false) // 12.9
    UUID id; // 12.10

    @Column(name = "email", nullable = false, columnDefinition = "text") // 12.11
    String email; // 12.12

    @Column(name = "email_normalized", nullable = false, columnDefinition = "text") // 12.13
    String emailNormalized; // 12.14

    @Column(name = "password_hash", nullable = false, columnDefinition = "text") // 12.15
    String passwordHash; // 12.16

    @Column(name = "status", nullable = false, length = 16) // 12.17
    String status; // 12.18

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz") // 12.19
    Instant createdAt; // 12.20

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz") // 12.21
    Instant updatedAt; // 12.22

    public UserEntity() {} // 12.23 JPA bắt buộc có constructor rỗng

    public UserEntity(UUID id, String email, String emailNormalized, String passwordHash, String status, Instant now) { // 12.24 Constructor khi tạo mới
        this.id = id; // 12.25
        this.email = email; // 12.26
        this.emailNormalized = emailNormalized; // 12.27
        this.passwordHash = passwordHash; // 12.28
        this.status = status; // 12.29
        this.createdAt = now; // 12.30
        this.updatedAt = now; // 12.31
    }

    public UUID getId() { return id; } // 12.32 Getter cho service (field package-private)
    public String getEmail() { return email; } // 12.33
    public String getPasswordHash() { return passwordHash; } // 12.34
    public String getStatus() { return status; } // 12.35
}