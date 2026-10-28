package com.cinema.cinema.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "seats")
public class SeatEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "auditorium_id", nullable = false)
    UUID auditoriumId;

    @Column(name = "row_label", nullable = false, length = 12)
    String rowLabel;

    @Column(name = "seat_number", nullable = false)
    Integer seatNumber;

    @Column(name = "seat_type", nullable = false, length = 24)
    String seatType;

    @Column(name = "is_active", nullable = false)
    Boolean isActive;

    @Column(name = "paired_seat_id")
    UUID pairedSeatId;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public SeatEntity() {}

    public SeatEntity(UUID id, UUID auditoriumId, String rowLabel, Integer seatNumber,
                      String seatType, Boolean isActive, UUID pairedSeatId, Instant now) {
        this.id = id;
        this.auditoriumId = auditoriumId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.isActive = isActive;
        this.pairedSeatId = pairedSeatId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getAuditoriumId() { return auditoriumId; }
    public Boolean getIsActive() { return isActive; }
    public UUID getPairedSeatId() { return pairedSeatId; }

    public UUID getId() { return id; } // 15.9
    public String getRowLabel() { return rowLabel; } // 15.10
    public Integer getSeatNumber() { return seatNumber; } // 15.11
    public String getSeatType() { return seatType; } // 15.12
}
