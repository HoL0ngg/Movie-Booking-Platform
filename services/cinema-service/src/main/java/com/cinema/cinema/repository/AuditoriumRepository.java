package com.cinema.cinema.repository; // 14.9

import java.util.List; // 14.10
import java.util.UUID; // 14.11
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.JpaRepository; // 14.12

import com.cinema.cinema.entity.AuditoriumEntity; // 14.13

public interface AuditoriumRepository extends JpaRepository<AuditoriumEntity, UUID> { // 14.14
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditoriumEntity> findForUpdateById(UUID id);
    List<AuditoriumEntity> findByCinemaIdOrderByNameAsc(UUID cinemaId); // 14.15 Các phòng của 1 rạp
}
