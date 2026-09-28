package com.cinema.booking.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeatEntity, ReservationSeatEntity.Key> {}
