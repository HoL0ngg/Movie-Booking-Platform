package com.cinema.cinema.repository; // 14.29

import java.time.Instant; // 14.30
import java.util.Collection; // 14.31
import java.util.List; // 14.32
import java.util.Optional; // 14.33
import java.util.UUID; // 14.34

import org.springframework.data.jpa.repository.JpaRepository; // 14.35
import org.springframework.data.jpa.repository.Lock; // 14.36
import org.springframework.data.jpa.repository.Query; // 14.37
import org.springframework.data.repository.query.Param; // 14.38

import com.cinema.cinema.entity.ShowtimeEntity; // 14.39

import jakarta.persistence.LockModeType; // 14.40

public interface ShowtimeRepository extends JpaRepository<ShowtimeEntity, UUID> { // 14.41

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 14.42 SELECT ... FOR UPDATE: 2 lệnh publish/cancel cùng suất phải xếp hàng
    Optional<ShowtimeEntity> findForUpdateById(UUID id); // 14.43 Tên method: "find...By" + Id

    @Query("select s from ShowtimeEntity s where s.auditoriumId in :auditoriumIds and s.status = 'PUBLISHED' " // 14.44
            + "and s.startsAt >= :rangeStart and s.startsAt < :rangeEnd order by s.startsAt") // 14.45 Khoảng [start, end)
    List<ShowtimeEntity> findPublishedInRange(@Param("auditoriumIds") Collection<UUID> auditoriumIds, // 14.46
                                              @Param("rangeStart") Instant rangeStart, // 14.47
                                              @Param("rangeEnd") Instant rangeEnd); // 14.48
}