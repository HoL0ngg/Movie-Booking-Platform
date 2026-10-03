package com.cinema.cinema.repository;

import com.cinema.cinema.entity.SeatEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<SeatEntity, UUID> {}
