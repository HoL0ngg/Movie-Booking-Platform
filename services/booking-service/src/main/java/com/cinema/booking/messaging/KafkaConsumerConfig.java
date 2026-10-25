package com.cinema.booking.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaConsumerConfig {

    /**
     * Lỗi tạm thời: retry 3 lần (cách nhau 1s) rồi chuyển sang quarantine topic.
     * PoisonMessageException: chuyển thẳng sang quarantine, không retry.
     * Spring Boot tự gắn CommonErrorHandler bean này vào listener container
     * factory.
     * Quarantine giữ nguyên key/value gốc và thêm header mô tả lỗi, có thể replay
     * thủ công.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template,
            @Value("${cinema.messaging.booking-quarantine-topic}") String quarantineTopic) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(quarantineTopic, -1));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(PoisonMessageException.class);
        return handler;
    }
}
