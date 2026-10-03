package com.cinema.booking.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "showtime_snapshots")
public class ShowtimeSnapshotEntity {
    @Id
    @Column(name = "showtime_id", nullable = false)
    UUID showtimeId;

    @Column(name = "cinema_id", nullable = false)
    UUID cinemaId;

    @Column(name = "auditorium_id", nullable = false)
    UUID auditoriumId;

    @Column(name = "movie_id", nullable = false)
    UUID movieId;

    @Column(name = "starts_at", nullable = false, columnDefinition = "timestamptz")
    Instant startsAt;

    @Column(name = "sales_close_at", nullable = false, columnDefinition = "timestamptz")
    Instant salesCloseAt;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Column(name = "status", nullable = false, length = 16)
    String status;

    @Column(name = "snapshot_version", nullable = false)
    Long snapshotVersion;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public ShowtimeSnapshotEntity() {}
}
