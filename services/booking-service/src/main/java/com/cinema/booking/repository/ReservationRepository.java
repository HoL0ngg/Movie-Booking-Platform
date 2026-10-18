package com.cinema.booking.repository;

import com.cinema.booking.entity.ReservationEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.id = :id")
    Optional<ReservationEntity> lockById(@Param("id") UUID id);

    Optional<ReservationEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query("select r.id from ReservationEntity r where r.status = 'HELD' and r.holdExpiresAt < :now order by r.holdExpiresAt")
    List<UUID> findExpiredHeldIds(@Param("now") Instant now, Pageable pageable);
}
