package com.cinema.payment.inbox;

import org.springframework.stereotype.Component;

import com.cinema.payment.messaging.BookingConfirmationFailedPayload;
import com.cinema.payment.messaging.EventEnvelope;
import com.cinema.payment.messaging.PoisonMessageException;
import com.cinema.payment.service.RefundService;

@Component
public class BookingEventHandler {
    private final RefundService refundService;

    public BookingEventHandler(RefundService refundService) {
        this.refundService = refundService;
    }

    // Đã thu tiền nhưng booking không xác định được ==> tạo yêu cầu hoàn
    // tiền(imdempotent)
    public void onBookingConfirmationFailed(EventEnvelope event) {
        BookingConfirmationFailedPayload p = event.payloadAs(BookingConfirmationFailedPayload.class);
        if (p.paymentId() == null) {
            throw new PoisonMessageException(event.eventType() + " thiếu paymentId (" + event.eventId() + ")");
        }
        refundService.requestRefund(p.paymentId(), "BOOKING_CONFIRMATION_FAILED: " + p.reason());
    }
}
