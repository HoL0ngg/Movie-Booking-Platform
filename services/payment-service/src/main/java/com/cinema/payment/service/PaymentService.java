package com.cinema.payment.service;

import static com.cinema.payment.entity.Statuses.*;

import com.cinema.payment.exception.ApiExceptionHandler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.cinema.payment.client.BookingClient;
import com.cinema.payment.client.ReservationSnapshot;
import com.cinema.payment.common.Hashes;
import com.cinema.payment.dto.CreatePaymentRequest;
import com.cinema.payment.dto.CreateResult;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.dto.RefundResponse;
import com.cinema.payment.entity.PaymentAttemptEntity;
import com.cinema.payment.entity.PaymentEntity;
import com.cinema.payment.entity.RefundEntity;
import com.cinema.payment.exception.ApiException;
import com.cinema.payment.provider.CheckoutRequest;
import com.cinema.payment.provider.CheckoutRules;
import com.cinema.payment.provider.PaymentProvider;
import com.cinema.payment.provider.PaymentProviderRegistry;
import com.cinema.payment.repository.PaymentAttemptRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.RefundRepository;

@Service
public class PaymentService {
    private static final Logger LOG = Logger.getLogger(PaymentService.class.getName());

    private static final String RESERVATION_HELD = "HELD";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final RefundRepository refundRepository;
    private final BookingClient bookingClient;
    private final PaymentProviderRegistry providers;
    private final TransactionTemplate tx;
    private final String defaultCurrency;

    public PaymentService(PaymentRepository paymentRepository,
            PaymentAttemptRepository attemptRepository,
            RefundRepository refundRepository,
            BookingClient bookingClient,
            PaymentProviderRegistry providers,
            TransactionTemplate tx,
            @Value("${payment.default-currency:VND}") String defaultCurrency) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.refundRepository = refundRepository;
        this.bookingClient = bookingClient;
        this.providers = providers;
        this.tx = tx;
        this.defaultCurrency = defaultCurrency;
    }

    /**
     * Tạo thanh toán cho một reservation. Idempotent theo (userId, Idempotency-Key)
     * và theo reservation.
     * Gọi booking-service TRƯỚC (ngoài transaction), sau đó mới ghi DB.
     */
    public CreateResult create(UUID userId, String idempotencyKey, CreatePaymentRequest req,
            String clientIp, String bearerToken) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 160) {
            throw new IllegalArgumentException("Idempotency-Key là bắt buộc (tối đa 160 ký tự)");
        }

        PaymentProvider provider = providers.resolve(req.provider());
        CheckoutRules rules = provider.rules();
        String currency = defaultCurrency;
        if (!rules.supportsCurrency(currency)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_CURRENCY",
                    "Phương thức " + provider.code() + " không hỗ trợ tiền tệ " + currency);
        }
        Map<String, String> options = options(req);
        // Khoá idempotency gắn với toàn bộ nội dung yêu cầu, gồm cả provider và tuỳ
        // chọn của nó.
        byte[] hash = Hashes.sha256(provider.code() + "|" + req.reservationId() + "|" + options);

        var byKey = paymentRepository.findByUserIdAndRequestKey(userId, idempotencyKey);
        if (byKey.isPresent()) {
            return replay(byKey.get(), hash);
        }
        var byReservation = paymentRepository.findByBookingId(req.reservationId());
        if (byReservation.isPresent()) {

            return existingForReservation(byReservation.get(), userId);
        }
        ReservationSnapshot reservationSnapshot = bookingClient.getReservation(req.reservationId(), bearerToken);
        Instant now = Instant.now();
        if (!RESERVATION_HELD.equals(reservationSnapshot.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "RESERVATION_NOT_PAYABLE",
                    "Reservation ở trạng thái " + reservationSnapshot.status() + ", không thể thanh toán");
        }
        long amount = reservationSnapshot.totalMinor();
        if (amount <= 0 || amount > rules.maxAmountMinor()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_AMOUNT",
                    "Số tiền thanh toán không hợp lệ");
        }

        try {
            UUID paymentId = tx.execute(status -> createInTx(provider, rules, userId, idempotencyKey, hash,
                    req.reservationId(), reservationSnapshot, amount, currency, options, clientIp, now));
            return new CreateResult(toResponse(paymentRepository.findById(paymentId).orElseThrow()), true);
        } catch (DataIntegrityViolationException race) {
            // Hai request đồng thời cùng key/reservation: thua cuộc thì trả về kết quả của
            // bên thắng.
            LOG.fine("Va chạm unique khi tạo payment, đọc lại kết quả của request thắng");
            var k = paymentRepository.findByUserIdAndRequestKey(userId, idempotencyKey);
            if (k.isPresent()) {
                return replay(k.get(), hash);
            }
            var r = paymentRepository.findByBookingId(req.reservationId());
            if (r.isPresent()) {
                return existingForReservation(r.get(), userId);
            }
            throw race;
        }
    }

    private UUID createInTx(PaymentProvider paymentProvider, CheckoutRules rules, UUID userId, String key,
            byte[] hash, UUID reservationId, ReservationSnapshot reservationSnapshot, long amount, String currency,
            Map<String, String> options, String clientIp, Instant now) {
        // Provider có thể chỉ có độ chính xác giây (vd. VNPay) nên created_at được làm
        // tròn xuống giây để
        // các lần querydr/refund sau này tái tạo đúng cùng một mốc thời gian.
        Instant createdAt = now.truncatedTo(ChronoUnit.SECONDS);
        Instant expireAt = earliest(
                reservationSnapshot.holdExpiresAt().minus(rules.holdSafetyMargin()),
                createdAt.plus(rules.paymentWindow()));
        PaymentEntity payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        // Booking chỉ được tạo SAU khi thanh toán xong nên chưa có booking_id thật;
        // reservationId là tham chiếu
        // logic ổn định và duy nhất (cột booking_id UNIQUE => tối đa một payment cho
        // mỗi reservation).
        payment.setBookingId(reservationId);
        payment.setReservationId(reservationId);
        payment.setUserId(userId);
        payment.setAmountMinor(amount);
        payment.setCurrency(currency);
        payment.setRequestKey(key);
        payment.setRequestHash(hash);
        payment.setStatus(PAY_REQUESTED);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentRepository.saveAndFlush(payment);

        PaymentAttemptEntity attempt = new PaymentAttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentId(payment.getId());
        attempt.setProvider(paymentProvider.code());
        attempt.setMerchantReference(attempt.getId().toString().replace("-", "")); // 32 ký tự
        attempt.setStatus(ATTEMPT_REQUESTED);
        attempt.setCreatedAt(createdAt);
        attempt.setUpdatedAt(now);
        String description = "Thanh toan ve xem phim " + reservationId.toString().substring(0, 8);
        attempt.setCheckoutUrl(paymentProvider.createCheckout(new CheckoutRequest(
                attempt.getMerchantReference(), amount, currency, description, clientIp,
                createdAt, expireAt, options)).url());

        payment.setStatus(PAY_PROVIDER_PENDING);
        return payment.getId();
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID userId, UUID paymentId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> notFound());
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> refunds(UUID userId, UUID paymentId) {
        PaymentEntity p = paymentRepository.findByIdAndUserId(paymentId, userId).orElseThrow(() -> notFound());
        return refundRepository.findByPaymentId(p.getId()).stream().map(this::toRefundResponse).toList();
    }

    /**
     * Trạng thái hiện tại theo mã tham chiếu của một lần thử thanh toán (dùng cho
     * trang quay về của provider).
     */
    @Transactional(readOnly = true)
    public PaymentResponse findByMerchantReference(String provider, String merchantReference) {
        return attemptRepository.findByProviderAndMerchantReference(provider, merchantReference)
                .flatMap(a -> paymentRepository.findById(a.getPaymentId()))
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Tuỳ chọn riêng của provider lấy từ request; TreeMap để thứ tự ổn định khi
     * băm.
     */
    private static Map<String, String> options(CreatePaymentRequest req) {
        Map<String, String> options = new TreeMap<>();
        if (req.bankCode() != null && !req.bankCode().isBlank()) {
            options.put("bankCode", req.bankCode());
        }
        if (req.locale() != null && !req.locale().isBlank()) {
            options.put("locale", req.locale());
        }
        return options;
    }

    private CreateResult replay(PaymentEntity existing, byte[] hash) {
        // So sánh hash hiện tại với hash đã lưu trong payment trước đó
        // Array.equal() trả về true nếu như 2 mảng byte có nội dung giống nhau
        if (!Arrays.equals(existing.getRequestHash(), hash)) {
            // Nếu hash khác nhau, nghĩa là cùng Idempotency-Key nhưng nội dung
            // request đã thay đổi, ví dụ: số tiền hoặc booking_id khác.
            // Trả về HTTP 422 UNPROCESSABLE_ENTITY để từ chối request.
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key đã được dùng với request khác");
        }

        /*
         * Nếu hash giống nhau, đây là request lặp lại có cùng nội dung
         * Trả về kết quả payment đã tồn tại, không tạo payment mới
         * Tham số false cho biết đây không phải là một payment mới
         */

        return new CreateResult(toResponse(existing), false);
    }

    private PaymentResponse toResponse(PaymentEntity p) {
        PaymentAttemptEntity attempt = attemptRepository.findFirstByPaymentIdOrderByCreatedAtDesc(p.getId())
                .orElse(null);
        PaymentResponse.Checkout checkout = null;
        String failureCode = null;
        if (attempt != null) {
            failureCode = attempt.getFailureCode();
            boolean open = PAY_REQUESTED.equals(p.getStatus()) || PAY_PROVIDER_PENDING.equals(p.getStatus());
            if (open && ATTEMPT_PROVIDER_PENDING.equals(attempt.getStatus()) && attempt.getCheckoutUrl() != null) {
                checkout = new PaymentResponse.Checkout(attempt.getProvider(), attempt.getCheckoutUrl());
            }
        }

        return new PaymentResponse(p.getId(), p.getReservationId(), p.getStatus(), p.getAmountMinor(), p.getCurrency(),
                checkout, failureCode, p.getCreatedAt(), p.getUpdatedAt());
    }

    private RefundResponse toRefundResponse(RefundEntity r) {
        return new RefundResponse(r.getId(), r.getPaymentId(), r.getStatus(), r.getAmountMinor(), r.getCurrency(),
                r.getFailureCode(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private CreateResult existingForReservation(PaymentEntity existing, UUID userId) {
        if (!existing.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "Không tìm thấy reservation");
        }

        if (PAY_REQUESTED.equals(existing.getStatus()) || PAY_PROVIDER_PENDING.equals(existing.getStatus())) {
            return new CreateResult(toResponse(existing), false); // thanh toán đang mở trả về url cũ
        }

        throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_ALREADY_EXISTS",
                "Reservation này đã có thanh toán ở trạng thái " + existing.getStatus());
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy payment");
    }
}
