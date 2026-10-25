package com.cinema.payment.outbox;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.cinema.payment.common.JsonUtils;
import com.cinema.payment.entity.OutboxEventEntity;
import com.cinema.payment.messaging.EventEnvelope;

@Component
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final long sendTimeoutMs;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
            @Value("${cinema.messaging.payment-topic}") String topic,
            @Value("${cinema.outbox.send-timeout-ms:5000}") long sendTimeoutMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Override
    public void publish(OutboxEventEntity e) throws Exception {
        var envelope = new EventEnvelope(e.getEventId(), e.getEventType(), e.getEventVersion(),
                e.getAggregateId(), e.getOccurredAt().toString(), e.getTraceId(),
                JsonUtils.readTree(e.getPayload()));
        var record = new ProducerRecord<>(topic, e.getAggregateId().toString(), JsonUtils.toJson(envelope));
        record.headers().add("event_id", e.getEventId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("event_type", e.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add("trace_id", e.getTraceId().getBytes(StandardCharsets.UTF_8));

        // Chờ broker ack (acks=all + idempotence đã cấu hình trong application.yml)
        kafkaTemplate.send(record).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
    }
}
