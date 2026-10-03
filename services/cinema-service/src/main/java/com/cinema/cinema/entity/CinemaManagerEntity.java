package com.cinema.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cinema_managers")
@IdClass(CinemaManagerEntity.Key.class)
public class CinemaManagerEntity {
    @Id
    @Column(name = "cinema_id", nullable = false)
    UUID cinemaId;

    @Id
    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "assigned_at", nullable = false, columnDefinition = "timestamptz")
    Instant assignedAt;

    public CinemaManagerEntity() {}

    public static class Key implements Serializable {
        public UUID cinemaId;
        public UUID userId;

        public Key() {}

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(cinemaId, key.cinemaId) && Objects.equals(userId, key.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cinemaId, userId);
        }
    }
}
