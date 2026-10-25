package com.cinema.payment.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gọi VNPay refund cho các yêu cầu đến hạn. Tắt bằng
 * payment.refund.dispatch-enabled=false.
 */
@Component
@ConditionalOnProperty(name = "payment.refund.dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class RefundDispatchJob {
    private static final Logger LOG = Logger.getLogger(RefundDispatchJob.class.getName());

    private final RefundService refundService;
    private final int batchSize;

    public RefundDispatchJob(RefundService refundService,
            @Value("${payment.refund.batch-size:20}") int batchSize) {
        this.refundService = refundService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${payment.refund.poll-delay-ms:10000}")
    public void run() {
        try {
            refundService.processDue(batchSize);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Refund dispatch job lỗi", e);
        }
    }
}
