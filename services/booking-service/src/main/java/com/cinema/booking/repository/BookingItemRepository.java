package com.cinema.booking.repository;

import com.cinema.booking.entity.BookingItemEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingItemRepository extends JpaRepository<BookingItemEntity, UUID> {

    List<BookingItemEntity> findAllByBookingIdOrderBySeatId(UUID bookingId);
}
