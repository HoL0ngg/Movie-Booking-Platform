package com.cinema.booking.inbox;

import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;
import com.cinema.booking.messaging.BookingConfirmationFailedPayload;
import com.cinema.booking.messaging.EventEnvelope;
import com.cinema.booking.messaging.EventTypes;
import com.cinema.booking.messaging.PaymentPayload;
import com.cinema.booking.messaging.PoisonMessageException;
import com.cinema.booking.service.BookingService;
import com.cinema.booking.service.OutboxService;

@Component
public class PaymentEventHandler {
    private static final Logger LOG = Logger.getLogger(PaymentEventHandler.class.getName());

    private final BookingService bookingService;
    private final OutboxService outboxService;

    public PaymentEventHandler(BookingService bookingService,
            OutboxService outboxService) {
        this.bookingService = bookingService;
        this.outboxService = outboxService;
    }

    public void onPaymentSucceeded(EventEnvelope event) {
        PaymentPayload p = requirePayload(event);

        BookingService.Failure failure = bookingService.confirmForPayment(
                p.bookingId(), p.reservationId(), p.paymentId(), p.amountMinor(), p.currency());
        if (failure != null) {
            LOG.warning("Không xác nhận được booking cho reservation " + p.reservationId()
                    + " sau khi thanh toán " + p.paymentId() + ": " + failure.reason());
            // Tiền đã thu nhưng không giữ được ghế => báo payment service hoàn tiền
            outboxService.record(EventTypes.BOOKING_CONFIRMATION_FAILED, 1, p.reservationId(),
                    new BookingConfirmationFailedPayload(p.reservationId(), p.paymentId(), failure.reason()));
        }
    }

    public void onPaymentFailed(EventEnvelope event) {
        PaymentPayload p = requirePayload(event);
        bookingService.failPayment(p.bookingId(), p.reservationId(), p.paymentId(),
                p.failureCode() == null ? "PAYMENT_FAILED" : p.failureCode());
    }

    private PaymentPayload requirePayload(EventEnvelope event) {
        PaymentPayload p = event.payloadAs(PaymentPayload.class);
        UUID reservationId = p.reservationId();
        if (reservationId == null || p.paymentId() == null || p.bookingId() == null) {
            throw new PoisonMessageException(
                    event.eventType() + " thiếu bookingId/reservationId/paymentId (" + event.eventId() + ")");
        }
        if (EventTypes.PAYMENT_SUCCEEDED.equals(event.eventType())
                && (p.amountMinor() <= 0 || p.currency() == null || p.currency().isBlank())) {
            throw new PoisonMessageException(
                    event.eventType() + " thiếu amountMinor/currency (" + event.eventId() + ")");
        }
        return p;
    }
}
