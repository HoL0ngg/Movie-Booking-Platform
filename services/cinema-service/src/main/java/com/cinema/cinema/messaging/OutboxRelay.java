package com.cinema.cinema.messaging; // 13.1

import java.time.Instant; // 13.2
import java.util.List; // 13.3
import java.util.concurrent.TimeUnit; // 13.4

import org.slf4j.Logger; // 13.5
import org.slf4j.LoggerFactory; // 13.6
import org.springframework.beans.factory.annotation.Value; // 13.7
import org.springframework.kafka.core.KafkaTemplate; // 13.8
import org.springframework.scheduling.annotation.Scheduled; // 13.9
import org.springframework.stereotype.Component; // 13.10
import org.springframework.transaction.PlatformTransactionManager; // 13.11
import org.springframework.transaction.support.TransactionTemplate; // 13.12

import com.cinema.cinema.entity.OutboxEventEntity; // 13.13
import com.cinema.cinema.repository.OutboxEventRepository; // 13.14

@Component // 13.15
public class OutboxRelay { // 13.16 Đẩy outbox_events chưa gửi lên Kafka

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class); // 13.17

    private final OutboxEventRepository outboxEventRepository; // 13.18
    private final KafkaTemplate<String, String> kafkaTemplate; // 13.19 Gửi message key/value dạng chuỗi
    private final TransactionTemplate transactionTemplate; // 13.20 Tự mở/đóng transaction bằng code
    private final String topic; // 13.21
    private final int batchSize; // 13.22

    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate, // 13.23
                       PlatformTransactionManager transactionManager, // 13.24
                       @Value("${cinema.messaging.showtime-topic}") String topic, // 13.25
                       @Value("${cinema.messaging.relay-batch-size:50}") int batchSize) { // 13.26 Mặc định 50
        this.outboxEventRepository = outboxEventRepository; // 13.27
        this.kafkaTemplate = kafkaTemplate; // 13.28
        this.transactionTemplate = new TransactionTemplate(transactionManager); // 13.29
        this.topic = topic; // 13.30
        this.batchSize = batchSize; // 13.31
    }

    @Scheduled(fixedDelayString = "${cinema.messaging.relay-delay-ms:1000}") // 13.32 Chạy lặp, chờ xong lượt trước rồi mới chờ tiếp (mặc định 1 giây)
    public void relay() { // 13.33
        transactionTemplate.executeWithoutResult(status -> { // 13.34 Claim + gửi + đánh dấu trong 1 transaction
            List<OutboxEventEntity> batch = outboxEventRepository.claimBatch(Instant.now(), batchSize); // 13.35 Lấy lô, khóa dòng, bỏ qua dòng worker khác đang giữ
            for (OutboxEventEntity event : batch) { // 13.36
                try { // 13.37
                    kafkaTemplate.send(topic, event.getAggregateId().toString(), EventJson.envelope(event)) // 13.38 Key = aggregateId để giữ thứ tự theo showtime
                            .get(10, TimeUnit.SECONDS); // 13.39 Chờ broker xác nhận (ack)
                    event.markPublished(Instant.now()); // 13.40 Chỉ đánh dấu sau khi có ack
                } catch (InterruptedException ex) { // 13.41 Tiến trình bị ngắt
                    Thread.currentThread().interrupt(); // 13.42 Khôi phục cờ ngắt
                    return; // 13.43 Dừng lượt này
                } catch (Exception ex) { // 13.44 Lỗi gửi/timeout
                    log.warn("Outbox publish failed eventId={} attempts={}", event.getEventId(), event.getPublishAttempts(), ex); // 13.45 Không log payload
                    event.markFailed(Instant.now()); // 13.46 Tăng attempts, lùi lịch thử lại
                }
            }
        });
    }
}