package com.cinema.payment.service;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cinema.payment.entity.PaymentAttemptEntity;
import com.cinema.payment.repository.PaymentAttemptRepository;
import static com.cinema.payment.entity.Statuses.PROVIDER_VNPAY;

/**
 * Định kỳ querydr các attempt còn PROVIDER_PENDING quá min-age. Tắt bằng
 * payment.reconcile.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "payment.reconcile.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentReconciliationJob {
    private static final Logger LOG = Logger.getLogger(PaymentReconciliationJob.class.getName());

    private final PaymentAttemptRepository attemptRepository;
    private final PaymentReconciliationService reconciliation;
    private final Duration minAge;
    private final int batchSize;

    public PaymentReconciliationJob(PaymentAttemptRepository attemptRepository,
            PaymentReconciliationService reconciliation,
            @Value("${payment.reconcile.min-age:PT60S}") Duration minAge,
            @Value("${payment.reconcile.batch-size:50}") int batchSize) {
        this.attemptRepository = attemptRepository;
        this.reconciliation = reconciliation;
        this.minAge = minAge;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${payment.reconcile.poll-delay-ms:30000}")
    public void run() {
        try {
            for (PaymentAttemptEntity a : attemptRepository.findPendingOlderThan(
                    PROVIDER_VNPAY, Instant.now().minus(minAge), PageRequest.of(0, batchSize))) {
                try {
                    reconciliation.reconcile(a.getId());
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Đối soát attempt " + a.getId() + " lỗi", e);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Reconciliation job lỗi", e);
        }
    }
}
