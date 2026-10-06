package com.cinema.cinema.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "showtimes")
public class ShowtimeEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "auditorium_id", nullable = false)
    UUID auditoriumId;

    @Column(name = "movie_id", nullable = false)
    UUID movieId;

    @Column(name = "starts_at", nullable = false, columnDefinition = "timestamptz")
    Instant startsAt;

    @Column(name = "ends_at", nullable = false, columnDefinition = "timestamptz")
    Instant endsAt;

    @Column(name = "sales_close_at", nullable = false, columnDefinition = "timestamptz")
    Instant salesCloseAt;

    @Column(name = "price_minor", nullable = false)
    Long priceMinor;

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

    public ShowtimeEntity() {}

        public ShowtimeEntity(UUID id, UUID auditoriumId, UUID movieId, Instant startsAt, Instant endsAt, // 15.13 Constructor tạo suất mới
                          Instant salesCloseAt, Long priceMinor, String currency, String status, Instant now) { // 15.14
        this.id = id; // 15.15
        this.auditoriumId = auditoriumId; // 15.16
        this.movieId = movieId; // 15.17
        this.startsAt = startsAt; // 15.18
        this.endsAt = endsAt; // 15.19
        this.salesCloseAt = salesCloseAt; // 15.20
        this.priceMinor = priceMinor; // 15.21
        this.currency = currency; // 15.22
        this.status = status; // 15.23
        this.snapshotVersion = 1L; // 15.24 Phiên bản snapshot khởi đầu (DB yêu cầu > 0)
        this.createdAt = now; // 15.25
        this.updatedAt = now; // 15.26
    }

    public void markPublished(Instant now) { // 15.27 Chuyển sang PUBLISHED
        this.status = "PUBLISHED"; // 15.28
        this.updatedAt = now; // 15.29
    }

    public void markCancelled(Instant now) { // 15.30 Chuyển sang CANCELLED
        this.status = "CANCELLED"; // 15.31
        this.snapshotVersion = this.snapshotVersion + 1; // 15.32 Tăng phiên bản để consumer áp dụng đúng một lần
        this.updatedAt = now; // 15.33
    }

    public UUID getId() { return id; } // 15.34
    public UUID getAuditoriumId() { return auditoriumId; } // 15.35
    public UUID getMovieId() { return movieId; } // 15.36
    public Instant getStartsAt() { return startsAt; } // 15.37
    public Instant getEndsAt() { return endsAt; } // 15.38
    public Instant getSalesCloseAt() { return salesCloseAt; } // 15.39
    public Long getPriceMinor() { return priceMinor; } // 15.40
    public String getCurrency() { return currency; } // 15.41
    public String getStatus() { return status; } // 15.42
    public Long getSnapshotVersion() { return snapshotVersion; } // 15.43
}
