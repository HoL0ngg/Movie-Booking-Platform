package com.cinema.cinema.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cinemas")
public class CinemaEntity {
    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    String name;

    @Column(name = "address", nullable = false, columnDefinition = "text")
    String address;

    @Column(name = "city", nullable = false, columnDefinition = "text")
    String city;

    @Column(name = "timezone", nullable = false, columnDefinition = "text")
    String timezone;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    Instant updatedAt;

    public CinemaEntity() {}
    
    public UUID getId() { return id; } // 15.1 Getter (field package-private nên service ở package khác không đọc trực tiếp được)
    public String getName() { return name; } // 15.2
    public String getAddress() { return address; } // 15.3
    public String getCity() { return city; } // 15.4
    public String getTimezone() { return timezone; } // 15.5
}
