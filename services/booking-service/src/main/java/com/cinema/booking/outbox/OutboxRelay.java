package com.cinema.booking.outbox;

import java.time.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.booking.entity.OutboxEventEntity;
import com.cinema.booking.repository.OutboxEventRepository;

@Component
public class OutboxRelay {
    private static final Logger LOG = Logger.getLogger(OutboxRelay.class.getName());
    private static final int ERROR_LOG_AFTER_ATTEMPTS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final EventPublisher publisher;
    private final int batchSize;
    private final Duration maxBackoff;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
            EventPublisher publisher,
            @Value("${cinema.outbox.batch-size:50}") int batchSize,
            @Value("${cinema.outbox.max-backoff:PT5M}") Duration maxBackoff) {
        this.outboxEventRepository = outboxEventRepository;
        this.publisher = publisher;
        this.batchSize = batchSize;
        this.maxBackoff = maxBackoff;
    }

    public int batchSize() {
        return batchSize;
    }

    /**
     * Một transaction cho cả batch: các row bị khoá (SKIP LOCKED) tới khi commit
     * nên không instance
     * nào khác gửi trùng. Gửi lỗi => tăng publish_attempts và dời next_attempt_at
     * theo backoff luỹ thừa;
     * các event khác trong batch vẫn đi tiếp. Delivery là at-least-once, vì vậy
     * consumer phải idempotent
     * (xem IdempotentEventProcessor). Thứ tự giữa các event của cùng aggregate
     * không được đảm bảo
     * tuyệt đối khi có retry.
     *
     * @return số event đã claim
     */
    @Transactional
    public int publishBatch() {
        List<OutboxEventEntity> events = outboxEventRepository.claimBatch(Instant.now(), batchSize);
        for (OutboxEventEntity e : events) {
            int attempts = e.getPublishAttempts() + 1;
            try {
                publisher.publish(e);
                e.setPublishedAt(Instant.now());
                e.setPublishAttempts(attempts);
            } catch (Exception ex) {
                Duration backoff = Duration.ofSeconds(1L << Math.min(attempts, 20));
                if (backoff.compareTo(maxBackoff) > 0) {
                    backoff = maxBackoff;
                }
                e.setPublishAttempts(attempts);
                e.setNextAttemptAt(Instant.now().plus(backoff));
                Level level = attempts >= ERROR_LOG_AFTER_ATTEMPTS ? Level.SEVERE : Level.WARNING;
                LOG.log(level, "Outbox event " + e.getEventId() + " (" + e.getEventType()
                        + ") gửi lỗi lần " + attempts + ", thử lại sau " + backoff, ex);
            }
        }
        // Dirty checking tự flush các thay đổi ở trên khi commit
        return events.size();
    }
}
