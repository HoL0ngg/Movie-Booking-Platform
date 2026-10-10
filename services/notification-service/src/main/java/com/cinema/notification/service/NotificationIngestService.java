package com.cinema.notification.service; // 5.1

import java.time.Instant; // 5.2
import java.util.Optional; // 5.3
import java.util.Set; // 5.4
import java.util.UUID; // 5.5

import org.slf4j.Logger; // 5.6
import org.slf4j.LoggerFactory; // 5.7
import org.springframework.dao.DataIntegrityViolationException; // 5.8
import org.springframework.stereotype.Service; // 5.9

import com.cinema.notification.client.UserDirectoryClient; // 5.10
import com.cinema.notification.entity.NotificationEntity; // 5.11
import com.cinema.notification.messaging.EventMessage; // 5.12
import com.cinema.notification.repository.NotificationRepository; // 5.13

@Service // 5.14
public class NotificationIngestService { // 5.15 Biến event thành bản ghi thông báo (không @Transactional ở đây, xem giải thích)

    private static final Logger log = LoggerFactory.getLogger(NotificationIngestService.class); // 5.16
    private static final String EMAIL = "EMAIL"; // 5.17 Kênh duy nhất (khớp CHECK ck_notifications_channel)
    // private static final Set<String> SUPPORTED = Set.of("BookingConfirmed", "BookingCancelled", "SeatHoldExpired", "RefundCompleted"); // 5.18 Event cần thông báo
    private static final Set<String> SUPPORTED = Set.of("BookingConfirmed", "BookingCancelled", "RefundCompleted");

    private final NotificationRepository notificationRepository; // 5.19
    private final UserDirectoryClient userDirectoryClient; // 5.20
    private final TemplateRenderer templateRenderer; // 5.21

    public NotificationIngestService(NotificationRepository notificationRepository, // 5.22
                                     UserDirectoryClient userDirectoryClient, TemplateRenderer templateRenderer) { // 5.23
        this.notificationRepository = notificationRepository; // 5.24
        this.userDirectoryClient = userDirectoryClient; // 5.25
        this.templateRenderer = templateRenderer; // 5.26
    }

    public void ingest(String message) { // 5.27 Gọi từ Kafka listener
        EventMessage event = EventMessage.parse(message); // 5.28 Sai định dạng → InvalidEventException → quarantine
        if (!SUPPORTED.contains(event.eventType())) { // 5.29 Event không thuộc diện thông báo
            return; // 5.30 Bỏ qua và commit offset
        }
        if (notificationRepository.existsBySourceEventIdAndChannel(event.eventId(), EMAIL)) { // 5.31 Đã xử lý event này rồi (Kafka giao lặp)
            return; // 5.32 Không tra email, không tạo bản ghi
        }
        UUID userId = event.uuid("userId"); // 5.33
        Optional<String> email = userDirectoryClient.findEmail(userId); // 5.34 Hỏi auth; lỗi mạng → ném lỗi để Kafka thử lại
        if (email.isEmpty()) { // 5.35 User không còn tồn tại
            log.warn("Skip notification: user not found eventId={} type={}", event.eventId(), event.eventType()); // 5.36
            return; // 5.37
        }
        TemplateRenderer.RenderedMessage content = templateRenderer.render(event); // 5.38 Dựng tiêu đề + nội dung
        try { // 5.39
            notificationRepository.saveAndFlush(new NotificationEntity(UUID.randomUUID(), event.eventId(), // 5.40 Lưu PENDING; dispatcher sẽ gửi
                    event.eventType(), userId, EMAIL, email.get(), content.subject(), content.body(), Instant.now())); // 5.41
        } catch (DataIntegrityViolationException ex) { // 5.42 Hai message trùng xử lý song song: unique (source_event_id, channel) chặn bản thứ hai
            log.debug("Duplicate notification ignored eventId={}", event.eventId()); // 5.43 Coi như đã xử lý
        }
    }
}