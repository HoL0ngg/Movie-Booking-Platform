package com.cinema.booking.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cinema.booking.exception.ApiException;
import com.cinema.booking.messaging.BookingConfirmedPayload;
import com.cinema.booking.messaging.EventTypes;
import com.cinema.booking.dto.BookingResponse;
import com.cinema.booking.dto.ReservedSeat;
import com.cinema.booking.entity.BookingEntity;
import com.cinema.booking.entity.BookingItemEntity;
import com.cinema.booking.entity.ReservationSeatEntity;
import com.cinema.booking.entity.ShowtimeSeatEntity;
import com.cinema.booking.repository.*;

import static com.cinema.booking.entity.Statuses.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    /**
     * Lỗi nghiệp vụ dự kiến khi xác nhận booking (không phải exception hệ thống).
     */
    public record Failure(HttpStatus status, String reason) {
    }

    private record Outcome(BookingResponse response, Failure failure) {
        static Outcome ok(BookingResponse response) {
            return new Outcome(response, null);
        }

        static Outcome fail(HttpStatus status, String reason) {
            return new Outcome(null, new Failure(status, reason));
        }
    }

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeSnapshotRepository showtimeSnapshotRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final OutboxService outboxService;

    public BookingService(ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            ShowtimeSeatRepository showtimeSeatRepository,
            ShowtimeSnapshotRepository showtimeSnapshotRepository,
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            OutboxService outboxService) {
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.showtimeSeatRepository = showtimeSeatRepository;
        this.showtimeSnapshotRepository = showtimeSnapshotRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.outboxService = outboxService;
    }

    // Confirm booking after paying successfully (REST). Idempotent theo
    // reservation.
    @Transactional
    public BookingResponse confirm(UUID userId, UUID reservationId, UUID paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("PaymentId is required");
        }
        Outcome outcome = doConfirm(reservationId, userId, paymentId);
        if (outcome.failure() != null) {
            throw new ApiException(outcome.failure().status(), outcome.failure().reason());
        }
        return outcome.response();
    }

    /**
     * Xác nhận booking khi nhận event PaymentSucceeded (không có user context).
     * Không ném exception khi lỗi nghiệp vụ: trả về Failure để caller phát event bù
     * trừ,
     * tránh làm transaction của event consumer bị rollback-only.
     *
     * @return null nếu thành công (hoặc đã xác nhận trước đó), ngược lại là lý do
     *         thất bại
     */
    @Transactional
    public Failure confirmForPayment(UUID reservationId, UUID paymentId) {
        return doConfirm(reservationId, null, paymentId).failure();
    }

    // userId == null => chạy ở chế độ hệ thống, bỏ qua kiểm tra chủ sở hữu
    @Transactional
    public Outcome doConfirm(UUID reservationId, UUID userId, UUID paymentId) {
        var found = reservationRepository.lockById(reservationId)
                .filter(r -> userId == null || r.getUserId().equals(userId));
        if (found.isEmpty()) {
            return Outcome.fail(HttpStatus.NOT_FOUND, "Không tìm thấy reservation");
        }
        var reservation = found.get();

        var existing = bookingRepository.findByReservationId(reservationId);
        if (existing.isPresent()) {
            if (!existing.get().getPaymentId().equals(paymentId)) {
                return Outcome.fail(HttpStatus.CONFLICT, "Reservation đã được xác nhận với payment khác");
            }
            return Outcome.ok(toResponse(existing.get()));
        }

        Instant now = Instant.now();
        if (!RES_HELD.equals(reservation.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Reservation ở trạng thái " + reservation.getStatus());
        }
        if (!reservation.getHoldExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "Reservation đã hết hạn giữ ghế");
        }
        var showtime = showtimeSnapshotRepository.findById(reservation.getShowtimeId()).orElseThrow();
        var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(reservationId);

        // Lock và kiểm tra TẤT CẢ ghế trước khi ghi bất cứ thứ gì:
        // lỗi nghiệp vụ không để lại dữ liệu dở dang và không cần exception
        List<ShowtimeSeatEntity> seats = new ArrayList<>();
        for (var i : items) {
            var found2 = showtimeSeatRepository.lockSeat(reservation.getShowtimeId(), i.getSeatId());
            if (found2.isEmpty()) {
                return Outcome.fail(HttpStatus.CONFLICT, "Không tìm thấy ghế " + i.getSeatId());
            }
            var seat = found2.get();
            if (!reservationId.equals(seat.getReservationId()) || !SEAT_HELD.equals(seat.getStatus())) {
                return Outcome.fail(HttpStatus.CONFLICT,
                        "Ghế " + seat.getSeatLabel() + " không còn thuộc reservation này");
            }
            seats.add(seat);
        }

        var booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setReservationId(reservationId);
        booking.setShowtimeId(reservation.getShowtimeId());
        booking.setUserId(userId);
        booking.setPaymentId(paymentId);
        booking.setAmountMinor(items.stream().mapToLong(ReservationSeatEntity::getPriceMinor).sum());
        booking.setCurrency(showtime.getCurrency());
        booking.setStatus(BOOKING_CONFIRMED);
        booking.setConfirmedAt(now);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        bookingRepository.saveAndFlush(booking);

        for (int idx = 0; idx < items.size(); idx++) {
            var i = items.get(idx);
            var seat = seats.get(idx);

            seat.setStatus(SEAT_BOOKED);
            seat.setBookingId(booking.getId());
            seat.setHoldExpiresAt(null);
            seat.setUpdatedAt(now);

            var bi = new BookingItemEntity();
            bi.setId(UUID.randomUUID());
            bi.setBookingId(booking.getId());
            bi.setShowtimeId(reservation.getShowtimeId());
            bi.setSeatId(i.getSeatId());
            bi.setPriceMinor(i.getPriceMinor()); // chốt giá lúc giữ ghế
            bi.setCreatedAt(now);
            bookingItemRepository.save(bi);
        }

        reservation.setStatus(RES_CONFIRMED);
        reservation.setUpdatedAt(now);

        // Cùng transaction với thay đổi dữ liệu => event chỉ tồn tại nếu booking commit
        // thành công
        outboxService.record(EventTypes.BOOKING_CONFIRMED, 1, booking.getId(), new BookingConfirmedPayload(
                booking.getId(), reservationId, booking.getShowtimeId(), booking.getUserId(), paymentId,
                booking.getAmountMinor(), booking.getCurrency(), now.toString(),
                items.stream().map(ReservationSeatEntity::getSeatId).toList()));

        return Outcome.ok(toResponse(booking));
    }

    @Transactional
    public BookingResponse get(UUID userId, UUID bookingId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy booking"));
    }

    public BookingResponse toResponse(BookingEntity b) {
        var items = bookingItemRepository.findAllByBookingIdOrderBySeatId(b.getId());
        Map<UUID, String> labels = new HashMap<>();
        showtimeSeatRepository.findAllByShowtimeIdAndSeatIdIn(b.getShowtimeId(),
                items.stream().map(BookingItemEntity::getSeatId).toList())
                .forEach(s -> labels.put(s.getSeatId(), s.getSeatLabel()));

        List<ReservedSeat> seats = items.stream()
                .map(i -> new ReservedSeat(i.getSeatId(), labels.get(i.getSeatId()), i.getPriceMinor()))
                .toList();

        return new BookingResponse(b.getId(), b.getReservationId(), b.getShowtimeId(), b.getStatus(),
                b.getAmountMinor(), b.getCurrency(), b.getConfirmedAt(), seats);
    }
}
