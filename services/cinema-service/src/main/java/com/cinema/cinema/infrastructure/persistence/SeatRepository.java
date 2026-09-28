package com.cinema.cinema.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<SeatEntity, UUID> {}
