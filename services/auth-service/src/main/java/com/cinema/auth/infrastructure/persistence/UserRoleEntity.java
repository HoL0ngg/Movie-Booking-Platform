package com.cinema.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleEntity.Key.class)
public class UserRoleEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Id
    @Column(name = "role_id", nullable = false)
    UUID roleId;

    @Column(name = "granted_at", nullable = false, columnDefinition = "timestamptz")
    Instant grantedAt;

    public UserRoleEntity() {}

    public static class Key implements Serializable {
        public UUID userId;
        public UUID roleId;
        public Key() {}
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(userId, key.userId) && Objects.equals(roleId, key.roleId);
        }
        @Override public int hashCode() { return Objects.hash(userId, roleId); }
    }
}
