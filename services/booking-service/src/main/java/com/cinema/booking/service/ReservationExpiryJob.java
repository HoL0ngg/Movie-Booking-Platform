package com.cinema.booking.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinema.booking.repository.ReservationRepository;

@Component
@ConditionalOnProperty(name = "cinema.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {
    private static final Logger LOG = Logger.getLogger(ReservationExpiryJob.class.getName());

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final int batchSize;
    private final int maxBatchesPerRun;

    public ReservationExpiryJob(ReservationRepository reservationRepository,
            ReservationService reservationService,
            @Value("${cinema.expiry.batch-size:100}") int batchSize,
            @Value("${cinema.expiry.max-batches-per-run:20}") int maxBatchesPerRun) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.batchSize = batchSize;
        this.maxBatchesPerRun = maxBatchesPerRun;
    }

    @Scheduled(fixedDelayString = "${cinema.expiry.poll-delay-ms:30000}")
    public void run() {
        int expired = 0;
        int failed = 0;

        for (int batch = 0; batch < maxBatchesPerRun; batch++) {
            List<UUID> ids = reservationRepository.findExpiredHeldIds(Instant.now(), PageRequest.of(0, batchSize));
            if (ids.isEmpty()) {
                break;
            }

            int succeededInBatch = 0;
            for (UUID id : ids) {
                try {
                    reservationService.expire(id);
                    succeededInBatch++;
                } catch (Exception e) {
                    failed++;
                    LOG.log(Level.WARNING, "Không thể expire reservation " + id, e);
                }
            }
            expired += succeededInBatch;

            // Dừng nếu batch chưa đầy (đã hết việc) hoặc không có reservation nào xử lý
            // được
            // (tránh lặp vô hạn trên cùng một nhóm reservation bị lỗi).
            if (ids.size() < batchSize || succeededInBatch == 0) {
                break;
            }
        }

        if (expired > 0 || failed > 0) {
            LOG.info("Reservation expiry: expired=" + expired + ", failed=" + failed);
        }
    }
}