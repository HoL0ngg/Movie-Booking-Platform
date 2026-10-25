package com.cinema.booking.repository;

import com.cinema.booking.entity.OutboxEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    /**
     * Khoá một batch event đến hạn. SKIP LOCKED cho phép nhiều instance chạy song
     * song
     * mà không gửi trùng và không chờ nhau. Khớp index
     * ix_outbox_events_next_attempt_at_created_at (partial: published_at IS NULL).
     * Phải gọi trong transaction.
     */
    @Query(value = """
            select * from outbox_events
            where published_at is null and next_attempt_at <= :now
            order by next_attempt_at, created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> claimBatch(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("delete from OutboxEventEntity e where e.publishedAt is not null and e.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
