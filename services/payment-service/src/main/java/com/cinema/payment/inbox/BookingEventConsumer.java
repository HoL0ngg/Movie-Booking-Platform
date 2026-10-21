package com.cinema.payment.inbox;

import java.util.logging.Logger;

import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cinema.payment.messaging.EventEnvelope;
import com.cinema.payment.messaging.EventTypes;
import com.cinema.payment.messaging.PoisonMessageException;

@Component
public class BookingEventConsumer {
    public static final String CONSUMER_NAME = "payment.booking-events"; // processed_events.consumer_name
    private static final Logger LOG = Logger.getLogger(BookingEventConsumer.class.getName());

    private final IdempotentEventProcessor processor;
    private final BookingEventHandler handler;

    public BookingEventConsumer(IdempotentEventProcessor processor, BookingEventHandler handler) {
        this.processor = processor;
        this.handler = handler;
    }

    @KafkaListener(id = "payment-booking-consumer", topics = "${cinema.messaging.booking-topic}", groupId = "payment-service.booking")

    public void onMessage(String message) {
        EventEnvelope e = EventEnvelope.parse(message);
        String traceId = e.traceId() != null ? e.traceId() : e.eventId().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            if (EventTypes.BOOKING_CONFIRMATION_FAILED.equals(e.eventType())) {
                if (e.eventVersion() != 1) {
                    throw new PoisonMessageException("Version " + e.eventVersion() + " chưa được hỗ trợ");
                }
                processor.process(CONSUMER_NAME, e.eventId(), () -> handler.onBookingConfirmationFailed(e));
            } else {
                LOG.fine(() -> "Bỏ qua event type " + e.eventType());
            }
        }
    }
}
