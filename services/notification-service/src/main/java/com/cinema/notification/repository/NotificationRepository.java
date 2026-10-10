package com.cinema.notification.repository; // 9.1

import com.cinema.notification.entity.NotificationEntity; // 9.2
import java.time.Instant; // 9.3
import java.util.List; // 9.4
import java.util.UUID; // 9.5
import org.springframework.data.jpa.repository.JpaRepository; // 9.6
import org.springframework.data.jpa.repository.Query; // 9.7
import org.springframework.data.repository.query.Param; // 9.8

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> { // 9.9

    boolean existsBySourceEventIdAndChannel(UUID sourceEventId, String channel); // 9.10 Kiểm tra event đã có thông báo chưa

    @Query(value = "select * from notifications where status in ('PENDING','FAILED') " // 9.11 Việc cần gửi
            + "and attempt_count < :maxAttempts and next_attempt_at <= :now " // 9.12 Chưa vượt số lần thử và đã đến hạn
            + "order by next_attempt_at limit :limit for update skip locked", nativeQuery = true) // 9.13 Khóa dòng, bỏ qua dòng worker khác đang giữ
    List<NotificationEntity> claimDue(@Param("now") Instant now, @Param("maxAttempts") int maxAttempts, @Param("limit") int limit); // 9.14
}