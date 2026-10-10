package com.cinema.notification.messaging; // 3.

import com.cinema.notification.service.NotificationIngestService; // 3.2
import org.springframework.kafka.annotation.KafkaListener; // 3.3
import org.springframework.stereotype.Component; // 3.4

@Component // 3.5
public class BookingEventConsumer { // 3.6

    private final NotificationIngestService ingestService; // 3.7

    public BookingEventConsumer(NotificationIngestService ingestService) { // 3.8
        this.ingestService = ingestService; // 3.9
    }

    @KafkaListener(topics = "${cinema.messaging.booking-topic}") // 3.10 Lắng nghe topic booking-events
    public void onMessage(String message) { // 3.11 Nhận nguyên văn JSON; offset chỉ commit khi method kết thúc không lỗi
        ingestService.ingest(message); // 3.12 Adapter mỏng, logic ở service
    }
}