package com.cinema.cinema.repository; // 14.16

import java.util.List; // 14.17
import java.util.UUID; // 14.18

import org.springframework.data.jpa.repository.JpaRepository; // 14.19
import org.springframework.data.jpa.repository.Query; // 14.20
import org.springframework.data.repository.query.Param; // 14.21

import com.cinema.cinema.entity.SeatEntity; // 14.22

public interface SeatRepository extends JpaRepository<SeatEntity, UUID> { // 14.23

    @Query("select count(s) from SeatEntity s where s.auditoriumId = :auditoriumId and s.isActive = true") // 14.24
    long countActiveByAuditoriumId(@Param("auditoriumId") UUID auditoriumId); // 14.25 Số ghế hoạt động

    @Query("select s from SeatEntity s where s.auditoriumId = :auditoriumId and s.isActive = true " // 14.26
            + "order by s.rowLabel, s.seatNumber") // 14.27 Thứ tự chuẩn: hàng rồi số ghế
    List<SeatEntity> findActiveByAuditoriumId(@Param("auditoriumId") UUID auditoriumId); // 14.28
}