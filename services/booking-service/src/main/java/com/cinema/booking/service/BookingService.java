package com.cinema.booking.service;

import org.springframework.data.domain.PageRequest;
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
import com.cinema.booking.entity.ReservationEntity;
import com.cinema.booking.entity.ReservationSeatEntity;
import com.cinema.booking.entity.ShowtimeSeatEntity;
import com.cinema.booking.messaging.BookingCancelledPayload;
import com.cinema.booking.repository.*;
import com.cinema.booking.messaging.PaymentRequestedPayload;
import com.cinema.booking.messaging.ReservationReleasedPayload;
import com.cinema.booking.messaging.SeatHoldExpiredPayload;

import static com.cinema.booking.entity.Statuses.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    /**
     * Lỗi nghiệp vụ dự kiến khi xác nhận booking (không phải exception hệ thống).
     */
    public record Failure(HttpStatus status, String reason) {
    }

    private record SeatReference(UUID bookingId, UUID showtimeId, UUID seatId, long priceMinor) {
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

    @Transactional
    public BookingResponse beginCheckout(UUID userId, UUID reservationId, String idempotencyKey, String clientIp) {
        if (idempotencyKey == null || idempotencyKey.length() < 8
                || idempotencyKey.length() > 128 || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key không hợp lệ");
        }

        var reservation = reservationRepository.lockById(reservationId)
                .filter(r -> r.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy reservation"));
        Instant now = Instant.now();
        var existing = bookingRepository.lockByReservationId(reservationId);
        if (existing.isPresent()) {
            BookingEntity booking = existing.get();
            if (BOOKING_PAYMENT_PENDING.equals(booking.getStatus())
                    && !reservation.getHoldExpiresAt().isAfter(now)
                    && RES_PAYMENT_PENDING.equals(reservation.getStatus())) {
                var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(reservationId);
                cancelPendingBooking(reservation, booking, items, "HOLD_EXPIRED", RES_EXPIRED);
                return toResponse(booking);
            }
            if (BOOKING_PAYMENT_PENDING.equals(booking.getStatus())
                    && RES_PAYMENT_PENDING.equals(reservation.getStatus())) {
                return toResponse(booking);
            }
            if (BOOKING_CONFIRMED.equals(booking.getStatus()) && RES_CONFIRMED.equals(reservation.getStatus())) {
                return toResponse(booking);
            }
            throw new ApiException(HttpStatus.CONFLICT, "Reservation đã kết thúc, không thể bắt đầu thanh toán");
        }

        if (!RES_HELD.equals(reservation.getStatus()) || !reservation.getHoldExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "Reservation đã hết hạn hoặc không còn giữ ghế");
        }

        var showtime = showtimeSnapshotRepository.findById(reservation.getShowtimeId()).orElseThrow();
        var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(reservationId);

        List<ShowtimeSeatEntity> seats = new ArrayList<>();
        for (var i : items) {
            var seat = showtimeSeatRepository.lockSeat(reservation.getShowtimeId(), i.getSeatId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Không tìm thấy ghế " + i.getSeatId()));
            if (!reservationId.equals(seat.getReservationId()) || !SEAT_HELD.equals(seat.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Ghế " + seat.getSeatLabel() + " không còn thuộc reservation này");
            }
            seats.add(seat);
        }

        long amountMinor = items.stream().mapToLong(ReservationSeatEntity::getPriceMinor).sum();
        if (amountMinor <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Reservation không có số tiền thanh toán hợp lệ");
        }

        var booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setReservationId(reservationId);
        booking.setShowtimeId(reservation.getShowtimeId());
        booking.setUserId(reservation.getUserId());
        UUID paymentId = UUID.randomUUID();
        booking.setPaymentId(paymentId);
        booking.setAmountMinor(amountMinor);
        booking.setCurrency(showtime.getCurrency());
        booking.setStatus(BOOKING_PAYMENT_PENDING);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        bookingRepository.saveAndFlush(booking);

        for (var seat : seats) {
            seat.setStatus(SEAT_PAYMENT_PENDING);
            seat.setBookingId(booking.getId());
            seat.setUpdatedAt(now);
        }

        reservation.setStatus(RES_PAYMENT_PENDING);
        reservation.setUpdatedAt(now);

        outboxService.record(EventTypes.PAYMENT_REQUESTED, 1, paymentId, new PaymentRequestedPayload(
                paymentId, booking.getId(), reservationId, booking.getUserId(), amountMinor, showtime.getCurrency(),
                idempotencyKey, now, reservation.getHoldExpiresAt(), "vnpay", null, "vn", clientIp));
        return toResponse(booking);
    }

    @Transactional
    public Failure confirmForPayment(UUID bookingId, UUID reservationId, UUID paymentId,
            long amountMinor, String currency, String paymentGateway) {
        var reservation = reservationRepository.lockById(reservationId).orElse(null);
        var booking = bookingRepository.lockByReservationId(reservationId).orElse(null);
        if (reservation == null || booking == null) {
            return new Failure(HttpStatus.NOT_FOUND, "Không tìm thấy booking/reservation đang chờ thanh toán");
        }
        if (!booking.getId().equals(bookingId) || !booking.getPaymentId().equals(paymentId)) {
            return new Failure(HttpStatus.CONFLICT, "Payment không khớp với booking đang chờ");
        }
        if (!booking.getUserId().equals(reservation.getUserId())
                || booking.getAmountMinor() != amountMinor
                || !booking.getCurrency().equals(currency)) {
            return new Failure(HttpStatus.CONFLICT, "Số tiền hoặc chủ sở hữu payment không khớp booking");
        }
        if (BOOKING_CONFIRMED.equals(booking.getStatus()) && RES_CONFIRMED.equals(reservation.getStatus())) {
            return null;
        }
        if (!BOOKING_PAYMENT_PENDING.equals(booking.getStatus())
                || !RES_PAYMENT_PENDING.equals(reservation.getStatus())) {
            return new Failure(HttpStatus.CONFLICT, "Booking không còn ở trạng thái chờ thanh toán");
        }

        var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(reservationId);
        List<ShowtimeSeatEntity> seats = new ArrayList<>();
        for (var item : items) {
            var seat = showtimeSeatRepository.lockSeat(reservation.getShowtimeId(), item.getSeatId()).orElse(null);
            if (seat == null || !reservationId.equals(seat.getReservationId())
                    || !bookingId.equals(seat.getBookingId()) || !SEAT_PAYMENT_PENDING.equals(seat.getStatus())) {
                cancelPendingBooking(reservation, booking, items, "SEAT_OWNERSHIP_LOST", RES_RELEASED);
                return new Failure(HttpStatus.CONFLICT, "Ghế không còn được giữ cho booking này");
            }
            seats.add(seat);
        }

        Instant now = Instant.now();
        if (!reservation.getHoldExpiresAt().isAfter(now)) {
            cancelPendingBooking(reservation, booking, items, "PAYMENT_SUCCEEDED_AFTER_HOLD_EXPIRY", RES_EXPIRED);
            return new Failure(HttpStatus.GONE, "Giữ ghế đã hết hạn trước khi xác nhận thanh toán");
        }
        if (items.stream().mapToLong(ReservationSeatEntity::getPriceMinor).sum() != amountMinor) {
            cancelPendingBooking(reservation, booking, items, "PAYMENT_AMOUNT_MISMATCH", RES_RELEASED);
            return new Failure(HttpStatus.CONFLICT, "Tổng tiền booking không khớp payment");
        }

        for (int idx = 0; idx < items.size(); idx++) {
            var item = items.get(idx);
            var seat = seats.get(idx);
            seat.setStatus(SEAT_BOOKED);
            seat.setHoldExpiresAt(null);
            seat.setUpdatedAt(now);

            var bookingItem = new BookingItemEntity();
            bookingItem.setId(UUID.randomUUID());
            bookingItem.setBookingId(bookingId);
            bookingItem.setShowtimeId(reservation.getShowtimeId());
            bookingItem.setSeatId(item.getSeatId());
            bookingItem.setPriceMinor(item.getPriceMinor());
            bookingItem.setCreatedAt(now);
            bookingItemRepository.save(bookingItem);
        }

        var showtime = showtimeSnapshotRepository.findById(booking.getShowtimeId())
                .orElseThrow(() -> new IllegalStateException(
                        "Showtime snapshot missing for confirmed booking " + bookingId));
        List<BookingConfirmedPayload.SeatDetail> seatDetails = new ArrayList<>();
        for (int idx = 0; idx < items.size(); idx++) {
            seatDetails.add(new BookingConfirmedPayload.SeatDetail(
                    items.get(idx).getSeatId(), seats.get(idx).getSeatLabel(), items.get(idx).getPriceMinor()));
        }

        booking.setStatus(BOOKING_CONFIRMED);
        booking.setConfirmedAt(now);
        booking.setUpdatedAt(now);
        reservation.setStatus(RES_CONFIRMED);
        reservation.setUpdatedAt(now);
        outboxService.record(EventTypes.BOOKING_CONFIRMED, 1, bookingId, new BookingConfirmedPayload(
                bookingId, reservationId, booking.getShowtimeId(), showtime.getMovieId(), showtime.getStartsAt(),
                booking.getUserId(), paymentId, paymentGateway, now,
                items.stream().map(ReservationSeatEntity::getSeatId).toList(), seatDetails,
                booking.getAmountMinor(), booking.getCurrency()));
        return null;
    }

    @Transactional
    public void failPayment(UUID bookingId, UUID reservationId, UUID paymentId, String reason) {
        var reservation = reservationRepository.lockById(reservationId).orElse(null);
        var booking = bookingRepository.lockByReservationId(reservationId).orElse(null);
        if (reservation == null || booking == null) {
            throw new com.cinema.booking.messaging.PoisonMessageException(
                    "PaymentFailed references an unknown booking/reservation");
        }
        if (!booking.getId().equals(bookingId) || !booking.getPaymentId().equals(paymentId)) {
            throw new com.cinema.booking.messaging.PoisonMessageException(
                    "PaymentFailed identifiers do not match the pending booking");
        }
        if (BOOKING_CONFIRMED.equals(booking.getStatus())) {
            return;
        }
        if (BOOKING_PAYMENT_PENDING.equals(booking.getStatus())
                && RES_PAYMENT_PENDING.equals(reservation.getStatus())) {
            var items = reservationSeatRepository.findAllByReservationIdOrderBySeatId(reservationId);
            cancelPendingBooking(reservation, booking, items, reason, RES_RELEASED);
        }
    }

    private void cancelPendingBooking(ReservationEntity reservation,
            BookingEntity booking, List<ReservationSeatEntity> items, String reason, String reservationStatus) {
        Instant now = Instant.now();
        for (var item : items) {
            showtimeSeatRepository.lockSeat(reservation.getShowtimeId(), item.getSeatId()).ifPresent(seat -> {
                boolean ownedHold = SEAT_HELD.equals(seat.getStatus()) && seat.getBookingId() == null;
                boolean ownedPending = SEAT_PAYMENT_PENDING.equals(seat.getStatus())
                        && booking.getId().equals(seat.getBookingId());
                if (reservation.getId().equals(seat.getReservationId()) && (ownedHold || ownedPending)) {
                    seat.setStatus(SEAT_AVAILABLE);
                    seat.setReservationId(null);
                    seat.setBookingId(null);
                    seat.setHoldExpiresAt(null);
                    seat.setUpdatedAt(now);
                }
            });
        }
        booking.setStatus(BOOKING_CANCELLED);
        booking.setUpdatedAt(now);
        reservation.setStatus(reservationStatus);
        reservation.setUpdatedAt(now);
        List<UUID> seatIds = items.stream().map(ReservationSeatEntity::getSeatId).toList();
        outboxService.record(EventTypes.BOOKING_CANCELLED, 1, booking.getId(), new BookingCancelledPayload(
                booking.getId(), reservation.getId(), booking.getPaymentId(), booking.getUserId(),
                booking.getShowtimeId(), seatIds, reason, now, booking.getAmountMinor(), booking.getCurrency()));
        if (RES_EXPIRED.equals(reservationStatus)) {
            outboxService.record(EventTypes.SEAT_HOLD_EXPIRED, 1, reservation.getId(),
                    new SeatHoldExpiredPayload(reservation.getId(), reservation.getUserId(),
                            reservation.getShowtimeId(), seatIds, now));
        } else {
            outboxService.record(EventTypes.RESERVATION_RELEASED, 1, reservation.getId(),
                    new ReservationReleasedPayload(reservation.getId(), booking.getId(),
                            booking.getShowtimeId(), seatIds, reason, now));
        }
    }

    @Transactional
    public BookingResponse get(UUID userId, UUID bookingId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy booking"));
    }

    @Transactional(readOnly = true)
    public BookingResponse getByPaymentId(UUID userId, UUID paymentId) {
        return bookingRepository.findByPaymentIdAndUserId(paymentId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Chưa tìm thấy booking cho payment"));
    }

    public BookingResponse toResponse(BookingEntity b) {
        var items = bookingItemRepository.findAllByBookingIdOrderBySeatId(b.getId());
        Map<UUID, String> labels = new HashMap<>();
        List<ReservedSeat> seats;
        if (items.isEmpty() && BOOKING_PAYMENT_PENDING.equals(b.getStatus())) {
            var reservationItems = reservationSeatRepository.findAllByReservationIdOrderBySeatId(b.getReservationId());
            showtimeSeatRepository.findAllByShowtimeIdAndSeatIdIn(b.getShowtimeId(),
                    reservationItems.stream().map(ReservationSeatEntity::getSeatId).toList())
                    .forEach(s -> labels.put(s.getSeatId(), s.getSeatLabel()));
            seats = reservationItems.stream()
                    .map(i -> new ReservedSeat(i.getSeatId(), labels.get(i.getSeatId()), i.getPriceMinor()))
                    .toList();
        } else {
            showtimeSeatRepository.findAllByShowtimeIdAndSeatIdIn(b.getShowtimeId(),
                    items.stream().map(BookingItemEntity::getSeatId).toList())
                    .forEach(s -> labels.put(s.getSeatId(), s.getSeatLabel()));
            seats = items.stream()
                    .map(i -> new ReservedSeat(i.getSeatId(), labels.get(i.getSeatId()), i.getPriceMinor()))
                    .toList();
        }
        return new BookingResponse(b.getId(), b.getReservationId(), b.getShowtimeId(), b.getPaymentId(), b.getStatus(),
                b.getAmountMinor(), b.getCurrency(), b.getConfirmedAt(), seats);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> list(UUID userId, int limit) {
        var bookings = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, Math.min(Math.max(limit, 1), 50)));
        if (bookings.isEmpty()) {
            return List.of();
        }

        var itemsByBooking = bookingItemRepository
                .findAllByBookingIdIn(bookings.stream().map(BookingEntity::getId).toList())
                .stream().collect(Collectors.groupingBy(BookingItemEntity::getBookingId));

        var reservationIdsWithoutItems = bookings.stream()
                .filter(booking -> itemsByBooking.getOrDefault(booking.getId(), List.of()).isEmpty())
                .map(BookingEntity::getReservationId)
                .distinct()
                .toList();
        var reservationSeatsByReservation = reservationIdsWithoutItems.isEmpty()
                ? Map.<UUID, List<ReservationSeatEntity>>of()
                : reservationSeatRepository
                        .findAllByReservationIdInOrderByReservationIdAscSeatIdAsc(reservationIdsWithoutItems)
                        .stream()
                        .collect(Collectors.groupingBy(ReservationSeatEntity::getReservationId));

        Map<UUID, List<SeatReference>> seatsByBooking = new HashMap<>();
        for (var booking : bookings) {
            var bookingItems = itemsByBooking.getOrDefault(booking.getId(), List.of());
            if (!bookingItems.isEmpty()) {
                seatsByBooking.put(booking.getId(), bookingItems.stream()
                        .map(item -> new SeatReference(item.getBookingId(), item.getShowtimeId(),
                                item.getSeatId(), item.getPriceMinor()))
                        .toList());
            } else {
                seatsByBooking.put(booking.getId(), reservationSeatsByReservation
                        .getOrDefault(booking.getReservationId(), List.of()).stream()
                        .map(item -> new SeatReference(booking.getId(), item.getShowtimeId(),
                                item.getSeatId(), item.getPriceMinor()))
                        .toList());
            }
        }

        // Resolve seat labels in batches, including retained reservation seats for pending/cancelled bookings.
        Map<UUID, String> labels = new HashMap<>();
        seatsByBooking.values().stream().flatMap(List::stream)
                .collect(Collectors.groupingBy(SeatReference::showtimeId,
                        Collectors.mapping(SeatReference::seatId, Collectors.toList())))
                .forEach((showtimeId, seatIds) -> showtimeSeatRepository
                        .findAllByShowtimeIdAndSeatIdIn(showtimeId, seatIds)
                        .forEach(s -> labels.put(s.getSeatId(), s.getSeatLabel())));

        return bookings.stream().map(b -> new BookingResponse(
                b.getId(), b.getReservationId(), b.getShowtimeId(), b.getPaymentId(), b.getStatus(),
                b.getAmountMinor(), b.getCurrency(), b.getConfirmedAt(),
                seatsByBooking.getOrDefault(b.getId(), List.of()).stream()
                        .map(seat -> new ReservedSeat(
                                seat.seatId(), labels.get(seat.seatId()), seat.priceMinor()))
                        .toList()))
                .toList();
    }

}
