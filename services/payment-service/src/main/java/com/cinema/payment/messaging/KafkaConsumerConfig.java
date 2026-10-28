package com.cinema.payment.messaging;

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
     * Cấu hình xử lý lỗi cho Kafka Consumer.
     *
     * - Lỗi thông thường: retry 3 lần, mỗi lần cách nhau 1 giây.
     * - PoisonMessageException: không retry, chuyển thẳng sang
     * quarantine topic.
     * - Khi hết số lần retry mà vẫn lỗi, message được chuyển sang
     * quarantine topic thông qua DeadLetterPublishingRecoverer.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template,
            @Value("${cinema.messaging.payment-quarantine-topic}") String quarantineTopic) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(quarantineTopic, -1));
        // 1000ms giữa các lần retry
        // 3: số lần retry bổ sung sau lần xử lí ban đầu
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(PoisonMessageException.class);
        return handler;
    }
}
