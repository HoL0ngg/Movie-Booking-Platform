package com.cinema.cinema.repository;

import com.cinema.cinema.entity.CinemaManagerEntity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaManagerRepository extends JpaRepository<CinemaManagerEntity, CinemaManagerEntity.Key> {
    boolean existsByCinemaIdAndUserId(UUID cinemaId, UUID userId);
}
