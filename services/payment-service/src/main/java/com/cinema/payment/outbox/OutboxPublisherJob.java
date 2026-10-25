package com.cinema.payment.outbox;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Poll outbox định kỳ. Tắt bằng cinema.outbox.enabled=false (vd. profile test).
 */
@Component
@ConditionalOnProperty(name = "cinema.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisherJob {
    private static final Logger LOG = Logger.getLogger(OutboxPublisherJob.class.getName());

    private final OutboxRelay relay;

    public OutboxPublisherJob(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${cinema.outbox.poll-delay-ms:500}")
    public void run() {
        try {
            int claimed;
            do {
                claimed = relay.publishBatch();
            } while (claimed == relay.batchSize()); // còn nhiều thì xả tiếp
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Outbox relay lỗi", e);
        }
    }
}
