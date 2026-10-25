package com.cinema.booking.inbox;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.booking.repository.ProcessedEventRepository;

@Component
public class IdempotentEventProcessor {
    private static final Logger LOG = Logger.getLogger(IdempotentEventProcessor.class.getName());

    private final ProcessedEventRepository processedEventRepository;

    public IdempotentEventProcessor(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /** @return true nếu event mới và đã xử lý; false nếu là event trùng. */
    @Transactional
    public boolean process(String consumerName, UUID eventId, Runnable work) {
        if (processedEventRepository.insertIfAbsent(consumerName, eventId, Instant.now()) == 0) {
            LOG.fine(() -> "Bỏ qua event trùng " + eventId + " (" + consumerName + ")");
            return false;
        }
        work.run();
        return true;
    }
}
