package com.cinema.booking.repository;

import com.cinema.booking.entity.ReservationSeatEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeatEntity, ReservationSeatEntity.Key> {}
