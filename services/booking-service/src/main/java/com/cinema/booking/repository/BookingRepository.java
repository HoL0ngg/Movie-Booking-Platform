package com.cinema.booking.repository;

import com.cinema.booking.entity.BookingEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByReservationId(UUID reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingEntity b where b.reservationId = :reservationId")
    Optional<BookingEntity> lockByReservationId(@Param("reservationId") UUID reservationId);

    Optional<BookingEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<BookingEntity> findByPaymentIdAndUserId(UUID paymentId, UUID userId);

    // BookingRepository
    List<BookingEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

}
