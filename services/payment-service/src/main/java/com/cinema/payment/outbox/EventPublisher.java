package com.cinema.payment.outbox;

import com.cinema.payment.entity.OutboxEventEntity;

public interface EventPublisher {
    void publish(OutboxEventEntity event) throws Exception;
}
