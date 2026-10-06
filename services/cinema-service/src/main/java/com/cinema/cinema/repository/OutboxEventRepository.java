package com.cinema.cinema.repository; // 14.49

import java.time.Instant; // 14.50
import java.util.List; // 14.51
import java.util.UUID; // 14.52

import org.springframework.data.jpa.repository.JpaRepository; // 14.53
import org.springframework.data.jpa.repository.Query; // 14.54
import org.springframework.data.repository.query.Param; // 14.55

import com.cinema.cinema.entity.OutboxEventEntity; // 14.56

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> { // 14.57

    @Query(value = "select * from outbox_events where published_at is null and next_attempt_at <= :now " // 14.58 SQL thuần: chưa gửi và đã đến hạn
            + "order by next_attempt_at, created_at limit :limit for update skip locked", nativeQuery = true) // 14.59 SKIP LOCKED: nhiều worker không giẫm chân nhau
    List<OutboxEventEntity> claimBatch(@Param("now") Instant now, @Param("limit") int limit); // 14.60
}