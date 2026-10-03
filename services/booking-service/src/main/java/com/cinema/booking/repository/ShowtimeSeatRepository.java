package com.cinema.booking.repository;

import com.cinema.booking.entity.ShowtimeSeatEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeatEntity, ShowtimeSeatEntity.Key> {
    // Call once per seat in the canonical UUID order, inside one transaction.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowtimeSeatEntity s where s.showtimeId = :showtimeId and s.seatId = :seatId")
    Optional<ShowtimeSeatEntity> lockSeat(@Param("showtimeId") UUID showtimeId, @Param("seatId") UUID seatId);
}
