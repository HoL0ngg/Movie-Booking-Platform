package com.cinema.booking.inbox;

import java.util.logging.Logger;

import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cinema.booking.messaging.EventEnvelope;
import com.cinema.booking.messaging.EventTypes;
import com.cinema.booking.messaging.PoisonMessageException;
import com.cinema.booking.messaging.ShowtimeCancelledPayload;
import com.cinema.booking.messaging.ShowtimeSnapshotPayload;

@Component
public class ShowtimeEventConsumer {
    public static final String CONSUMER_NAME = "booking.showtime-events"; // processed_events.consumer_name
    private static final Logger LOG = Logger.getLogger(ShowtimeEventConsumer.class.getName());

    private final IdempotentEventProcessor processor;
    private final ShowtimeSnapshotSyncService syncService;

    public ShowtimeEventConsumer(IdempotentEventProcessor processor, ShowtimeSnapshotSyncService syncService) {
        this.processor = processor;
        this.syncService = syncService;
    }

    @KafkaListener(id = "booking-showtime-consumer", topics = "${cinema.messaging.showtime-topic}", groupId = "booking-service.showtime")
    public void onMessage(String message) {
        EventEnvelope e = EventEnvelope.parse(message);

        String traceId = e.traceId() != null ? e.traceId() : e.eventId().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            switch (e.eventType()) {
                case "ShowtimePublished" -> {
                    ShowtimeSnapshotPayload payload = e.payloadAs(ShowtimeSnapshotPayload.class);
                    boolean applied = processor.process(CONSUMER_NAME, e.eventId(),
                            () -> syncService.apply(payload, "PUBLISHED"));
                    LOG.info(() -> (applied ? "Đã đồng bộ showtime " : "Event trùng, bỏ qua showtime ")
                            + payload.showtimeId());
                }
                case "ShowtimeCancelled" -> {
                    ShowtimeCancelledPayload payload = e.payloadAs(ShowtimeCancelledPayload.class);
                    boolean applied = processor.process(CONSUMER_NAME, e.eventId(),
                            () -> syncService.cancel(payload));
                    LOG.info(() -> (applied ? "Đã huỷ showtime " : "Event trùng, bỏ qua huỷ showtime ")
                            + payload.showtimeId());
                }
                default -> LOG.warning(() -> "BỎ QUA event type '" + e.eventType() + "'");
            }
            if (e.eventVersion() != 1) {
                throw new PoisonMessageException("Version " + e.eventVersion() + " chưa được hỗ trợ");
            }
            // ShowtimeSnapshotPayload payload = e.payloadAs(ShowtimeSnapshotPayload.class);
            // processor.process(CONSUMER_NAME, e.eventId(), () ->
            // syncService.apply(payload));
        }

    }
}
