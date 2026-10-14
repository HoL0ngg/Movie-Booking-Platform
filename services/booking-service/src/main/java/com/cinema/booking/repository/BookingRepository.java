package com.cinema.booking.repository;

import com.cinema.booking.entity.BookingEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByReservationId(UUID reservationId);

    Optional<BookingEntity> findByIdAndUserId(UUID id, UUID userId);
}
