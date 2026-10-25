package com.cinema.payment.service;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.payment.repository.OutboxEventRepository;
import com.cinema.payment.repository.ProcessedEventRepository;

/**
 * Dọn outbox đã gửi và processed_events cũ. processed-retention phải dài hơn
 * thời gian broker có thể redeliver.
 */
@Component
public class HousekeepingJob {
    private static final Logger LOG = Logger.getLogger(HousekeepingJob.class.getName());

    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final Duration outboxRetention;
    private final Duration processedRetention;

    public HousekeepingJob(OutboxEventRepository outboxEventRepository,
            ProcessedEventRepository processedEventRepository,
            @Value("${cinema.housekeeping.outbox-retention:P7D}") Duration outboxRetention,
            @Value("${cinema.housekeeping.processed-retention:P14D}") Duration processedRetention) {
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxRetention = outboxRetention;
        this.processedRetention = processedRetention;
    }

    @Scheduled(cron = "${cinema.housekeeping.cron:0 15 * * * *}")
    @Transactional
    public void run() {
        Instant now = Instant.now();
        int outbox = outboxEventRepository.deletePublishedBefore(now.minus(outboxRetention));
        int processed = processedEventRepository.deleteProcessedBefore(now.minus(processedRetention));
        LOG.info("Housekeeping: outbox_events=" + outbox + ", processed_events=" + processed);
    }
}
