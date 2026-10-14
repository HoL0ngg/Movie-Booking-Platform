package com.cinema.booking.repository;

import com.cinema.booking.entity.ReservationSeatEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeatEntity, ReservationSeatEntity.Key> {
    List<ReservationSeatEntity> findAllByReservationIdOrderBySeatId(UUID reservationId);
}
