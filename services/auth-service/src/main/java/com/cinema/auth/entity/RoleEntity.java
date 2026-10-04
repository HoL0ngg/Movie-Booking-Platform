package com.cinema.auth.entity; // 12.36

import jakarta.persistence.*; // 12.37
import java.time.Instant; // 12.38
import java.util.UUID; // 12.39

@Entity // 12.40
@Table(name = "roles") // 12.41
public class RoleEntity { // 12.42
    @Id // 12.43
    @Column(name = "id", nullable = false) // 12.44
    UUID id; // 12.45

    @Column(name = "code", nullable = false, length = 40) // 12.46
    String code; // 12.47

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz") // 12.48
    Instant createdAt; // 12.49

    public RoleEntity() {} // 12.50

    public UUID getId() { return id; } // 12.51
    public String getCode() { return code; } // 12.52
}