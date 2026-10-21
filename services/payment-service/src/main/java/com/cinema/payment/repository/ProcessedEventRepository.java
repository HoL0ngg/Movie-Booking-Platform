package com.cinema.payment.repository;

import com.cinema.payment.entity.ProcessedEventEntity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.Key> {

    /** @return 1 nếu đánh dấu thành công (event mới), 0 nếu đã xử lý trước đó. */
    @Modifying
    @Query(value = """
            insert into processed_events (consumer_name, event_id, processed_at)
            values (:consumerName, :eventId, :processedAt)
            on conflict (consumer_name, event_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("consumerName") String consumerName,
            @Param("eventId") UUID eventId,
            @Param("processedAt") Instant processedAt);

    @Modifying
    @Query("delete from ProcessedEventEntity p where p.processedAt < :cutoff")
    int deleteProcessedBefore(@Param("cutoff") Instant cutoff);
}
