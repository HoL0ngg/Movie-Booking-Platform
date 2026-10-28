package com.cinema.booking.outbox;

import com.cinema.booking.entity.OutboxEventEntity;

/**
 * Abstraction cho message broker. Ném exception nếu broker chưa xác nhận đã
 * nhận event.
 */
public interface EventPublisher {
    void publish(OutboxEventEntity event) throws Exception;
}
