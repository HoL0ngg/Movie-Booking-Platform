package com.cinema.payment.inbox;

import org.springframework.stereotype.Component;

import com.cinema.payment.messaging.BookingConfirmationFailedPayload;
import com.cinema.payment.messaging.EventEnvelope;
import com.cinema.payment.messaging.PaymentRequestedPayload;
import com.cinema.payment.messaging.PoisonMessageException;
import com.cinema.payment.service.RefundService;
import com.cinema.payment.service.PaymentService;

@Component
public class BookingEventHandler {
    private final RefundService refundService;
    private final PaymentService paymentService;

    public BookingEventHandler(RefundService refundService, PaymentService paymentService) {
        this.refundService = refundService;
        this.paymentService = paymentService;
    }

    // Đã thu tiền nhưng booking không xác định được ==> tạo yêu cầu hoàn
    // tiền(idempotent)
    public void onBookingConfirmationFailed(EventEnvelope event) {
        BookingConfirmationFailedPayload p = event.payloadAs(BookingConfirmationFailedPayload.class);
        if (p.paymentId() == null) {
            throw new PoisonMessageException(event.eventType() + " thiếu paymentId (" + event.eventId() + ")");
        }
        refundService.requestRefund(p.paymentId(), "BOOKING_CONFIRMATION_FAILED: " + p.reason());
    }

    public void onPaymentRequested(EventEnvelope event) {
        PaymentRequestedPayload p = event.payloadAs(PaymentRequestedPayload.class);
        if (p.paymentId() == null || p.bookingId() == null || p.reservationId() == null || p.userId() == null
                || p.amountMinor() <= 0 || p.currency() == null || p.idempotencyKey() == null
                || p.holdExpiresAt() == null) {
            throw new PoisonMessageException(event.eventType() + " thiếu thông tin bắt buộc (" + event.eventId() + ")");
        }
        if (!p.paymentId().equals(event.aggregateId())) {
            throw new PoisonMessageException(event.eventType() + " aggregateId không khớp paymentId ("
                    + event.eventId() + ")");
        }
        paymentService.createFromBookingRequest(p);
    }
}
