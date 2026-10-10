package com.cinema.notification.config; // 2.1

import org.apache.kafka.common.TopicPartition; // 2.2
import org.springframework.beans.factory.annotation.Value; // 2.3
import org.springframework.context.annotation.Bean; // 2.4
import org.springframework.context.annotation.Configuration; // 2.5
import org.springframework.kafka.core.KafkaTemplate; // 2.6
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer; // 2.7
import org.springframework.kafka.listener.DefaultErrorHandler; // 2.8 Đẩy message lỗi sang topic quarantine
import org.springframework.util.backoff.FixedBackOff; // 2.9 Xử lý lỗi của listener

import com.cinema.notification.exception.InvalidEventException; // 2.10 Chính sách chờ giữa các lần thử lại

@Configuration(proxyBeanMethods = false) // 2.11
public class KafkaConsumerConfiguration { // 2.12

    @Bean // 2.13 Spring Boot tự gắn bean CommonErrorHandler vào mọi @KafkaListener
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate, // 2.14 Template gửi message quarantine
                                          @Value("${cinema.messaging.booking-topic}") String bookingTopic, // 2.15
                                          @Value("${cinema.messaging.booking-quarantine-topic}") String bookingQuarantine, // 2.16
                                          @Value("${cinema.messaging.payment-quarantine-topic}") String paymentQuarantine) { // 2.17
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, // 2.18 Hết lần thử → gửi vào quarantine
                (record, ex) -> new TopicPartition( // 2.19 Chọn topic đích theo topic gốc
                        record.topic().equals(bookingTopic) ? bookingQuarantine : paymentQuarantine, -1)); // 2.20 -1 = để Kafka tự chọn partition
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 4L)); // 2.21 Thử lại 4 lần, cách nhau 2 giây
        handler.addNotRetryableExceptions(InvalidEventException.class); // 2.22 Event sai định dạng: không thử lại, vào quarantine ngay
        return handler; // 2.23
    }
}