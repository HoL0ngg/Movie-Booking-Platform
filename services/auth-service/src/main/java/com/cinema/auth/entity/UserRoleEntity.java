package com.cinema.auth.entity; // 12.53

import java.io.Serializable; // 12.54
import java.time.Instant; // 12.55
import java.util.Objects; // 12.56
import java.util.UUID; // 12.57

import jakarta.persistence.Column; // 12.58
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity // 12.59
@Table(name = "user_roles") // 12.60
@IdClass(UserRoleEntity.Key.class) // 12.61 Khóa chính kép (user_id, role_id)
public class UserRoleEntity { // 12.62
    @Id // 12.63
    @Column(name = "user_id", nullable = false) // 12.64
    UUID userId; // 12.65

    @Id // 12.66
    @Column(name = "role_id", nullable = false) // 12.67
    UUID roleId; // 12.68

    @Column(name = "granted_at", nullable = false, columnDefinition = "timestamptz") // 12.69
    Instant grantedAt; // 12.70

    public UserRoleEntity() {} // 12.71

    public UserRoleEntity(UUID userId, UUID roleId, Instant grantedAt) { // 12.72
        this.userId = userId; // 12.73
        this.roleId = roleId; // 12.74
        this.grantedAt = grantedAt; // 12.75
    }

    public static class Key implements Serializable { // 12.76 Lớp khóa kép
        public UUID userId; // 12.77
        public UUID roleId; // 12.78
        public Key() {} // 12.79
        @Override public boolean equals(Object other) { // 12.80
            if (this == other) return true; // 12.81
            if (!(other instanceof Key key)) return false; // 12.82
            return Objects.equals(userId, key.userId) && Objects.equals(roleId, key.roleId); // 12.83
        }
        @Override public int hashCode() { return Objects.hash(userId, roleId); } // 12.84
    }
}