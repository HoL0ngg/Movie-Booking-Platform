package com.cinema.booking.inbox;

import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.cinema.booking.messaging.EventEnvelope;
import com.cinema.booking.messaging.EventTypes;

import java.util.logging.Logger;

@Component
public class PaymentEventConsumer {
    public static final String CONSUMER_NAME = "booking.payment-events"; // processed_events.consumer_name
    private static final Logger LOG = Logger.getLogger(PaymentEventConsumer.class.getName());

    private final IdempotentEventProcessor processor;
    private final PaymentEventHandler handler;

    public PaymentEventConsumer(IdempotentEventProcessor processor, PaymentEventHandler handler) {
        this.processor = processor;
        this.handler = handler;
    }

    @KafkaListener(id = "booking-payment-consumer", topics = "${cinema.messaging.payment-topic}", groupId = "booking-service.payment")
    public void onMessage(String message) {
        EventEnvelope e = EventEnvelope.parse(message);

        String traceId = e.traceId() != null ? e.traceId() : e.eventId().toString();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            switch (e.eventType()) {
                case EventTypes.PAYMENT_SUCCEEDED ->
                    processor.process(CONSUMER_NAME, e.eventId(), () -> handler.onPaymentSucceeded(e)); // insert into
                                                                                                        // processed_events
                case EventTypes.PAYMENT_FAILED ->
                    processor.process(CONSUMER_NAME, e.eventId(), () -> handler.onPaymentFailed(e));
                default -> LOG.fine(() -> "Bỏ qua event type " + e.eventType());
            }
        }

    }
}
