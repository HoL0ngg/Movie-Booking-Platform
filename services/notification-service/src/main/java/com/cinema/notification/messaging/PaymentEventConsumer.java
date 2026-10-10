package com.cinema.notification.messaging; // 3.13

import org.springframework.kafka.annotation.KafkaListener; // 3.14
import org.springframework.stereotype.Component; // 3.15

import com.cinema.notification.service.NotificationIngestService; // 3.16

@Component // 3.17
public class PaymentEventConsumer { // 3.18

    private final NotificationIngestService ingestService; // 3.19

    public PaymentEventConsumer(NotificationIngestService ingestService) { // 3.20
        this.ingestService = ingestService; // 3.21
    }

    @KafkaListener(topics = "${cinema.messaging.payment-topic}") // 3.22 Lắng nghe topic payment-events
    public void onMessage(String message) { // 3.23
        ingestService.ingest(message); // 3.24
    }
}