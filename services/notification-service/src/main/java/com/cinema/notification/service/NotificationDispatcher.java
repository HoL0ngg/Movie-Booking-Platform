package com.cinema.notification.service; // 10.1

import java.time.Instant; // 10.2
import java.util.List; // 10.3

import org.slf4j.Logger; // 10.4
import org.slf4j.LoggerFactory; // 10.5
import org.springframework.beans.factory.annotation.Value; // 10.6
import org.springframework.scheduling.annotation.Scheduled; // 10.7
import org.springframework.stereotype.Component; // 10.8
import org.springframework.transaction.PlatformTransactionManager; // 10.9
import org.springframework.transaction.support.TransactionTemplate; // 10.10

import com.cinema.notification.client.EmailSender; // 10.11
import com.cinema.notification.entity.NotificationEntity; // 10.12
import com.cinema.notification.repository.NotificationRepository; // 10.13

@Component // 10.14
public class NotificationDispatcher { // 10.15 Chạy nền: gửi các thông báo đến hạn

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class); // 10.16

    private final NotificationRepository notificationRepository; // 10.17
    private final EmailSender emailSender; // 10.18 Bean log hoặc smtp tùy cấu hình
    private final TransactionTemplate transactionTemplate; // 10.19
    private final int batchSize; // 10.20
    private final int maxAttempts; // 10.21

    public NotificationDispatcher(NotificationRepository notificationRepository, EmailSender emailSender, // 10.22
                                  PlatformTransactionManager transactionManager, // 10.23
                                  @Value("${notification.dispatch.batch-size:20}") int batchSize, // 10.24
                                  @Value("${notification.dispatch.max-attempts:5}") int maxAttempts) { // 10.25
        this.notificationRepository = notificationRepository; // 10.26
        this.emailSender = emailSender; // 10.27
        this.transactionTemplate = new TransactionTemplate(transactionManager); // 10.28
        this.batchSize = batchSize; // 10.29
        this.maxAttempts = maxAttempts; // 10.30
    }

    @Scheduled(fixedDelayString = "${notification.dispatch.delay-ms:2000}") // 10.31 Chạy lặp, xong lượt trước mới đếm giờ
    public void dispatch() { // 10.32
        transactionTemplate.executeWithoutResult(status -> { // 10.33 Claim + gửi + cập nhật trong 1 transaction
            List<NotificationEntity> due = notificationRepository.claimDue(Instant.now(), maxAttempts, batchSize); // 10.34
            for (NotificationEntity notification : due) { // 10.35
                try { // 10.36
                    emailSender.send(notification.getRecipientAddress(), notification.getSubject(), notification.getBody()); // 10.37 Gửi thật hoặc log
                    notification.markSent(Instant.now(), notification.getId().toString()); // 10.38 Dùng id thông báo làm mã tham chiếu
                } catch (Exception ex) { // 10.39 Lỗi gửi không ảnh hưởng booking/payment
                    log.warn("Notification send failed id={} attempts={} error={}", // 10.40 Chỉ log id và loại lỗi, không log email/nội dung
                            notification.getId(), notification.getAttemptCount(), ex.getClass().getSimpleName()); // 10.41
                    notification.markFailed(Instant.now(), ex.getClass().getSimpleName()); // 10.42 Tăng attempts, lùi lịch thử lại
                }
            }
        });
    }
}