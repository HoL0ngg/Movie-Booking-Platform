package com.cinema.payment.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventEntity.Key.class)
public class ProcessedEventEntity {
    @Id
    @Column(name = "consumer_name", nullable = false, length = 80)
    String consumerName;

    @Id
    @Column(name = "event_id", nullable = false)
    UUID eventId;

    @Column(name = "processed_at", nullable = false, columnDefinition = "timestamptz")
    Instant processedAt;

    public ProcessedEventEntity() {}

    public static class Key implements Serializable {
        public String consumerName;
        public UUID eventId;
        public Key() {}
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(consumerName, key.consumerName) && Objects.equals(eventId, key.eventId);
        }
        @Override public int hashCode() { return Objects.hash(consumerName, eventId); }
    }
}
