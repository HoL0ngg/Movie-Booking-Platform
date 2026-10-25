package com.cinema.payment.service;

import static com.cinema.payment.entity.Statuses.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.payment.common.Hashes;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.dto.RefundResponse;
import com.cinema.payment.entity.PaymentAttemptEntity;
import com.cinema.payment.entity.PaymentEntity;
import com.cinema.payment.entity.RefundEntity;
import com.cinema.payment.exception.ApiException;
import com.cinema.payment.messaging.EventTypes;
import com.cinema.payment.messaging.PaymentFailedPayload;
import com.cinema.payment.messaging.PaymentRequestedPayload;
import com.cinema.payment.messaging.PoisonMessageException;
import com.cinema.payment.service.OutboxService;
import com.cinema.payment.provider.CheckoutRequest;
import com.cinema.payment.provider.CheckoutRules;
import com.cinema.payment.provider.PaymentProvider;
import com.cinema.payment.provider.PaymentProviderRegistry;
import com.cinema.payment.repository.PaymentAttemptRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.RefundRepository;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final RefundRepository refundRepository;
    private final PaymentProviderRegistry providers;
    private final OutboxService outboxService;

    public PaymentService(PaymentRepository paymentRepository,
            PaymentAttemptRepository attemptRepository,
            RefundRepository refundRepository,
            PaymentProviderRegistry providers,
            OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.refundRepository = refundRepository;
        this.providers = providers;
        this.outboxService = outboxService;
    }

    @Transactional
    public void createFromBookingRequest(PaymentRequestedPayload request) {
        if (request.paymentId() == null || request.bookingId() == null || request.reservationId() == null
                || request.userId() == null || request.idempotencyKey() == null
                || request.idempotencyKey().isBlank() || request.idempotencyKey().length() > 160
                || request.amountMinor() <= 0 || request.currency() == null
                || !request.currency().matches("[A-Z]{3}") || request.holdExpiresAt() == null) {
            throw new PoisonMessageException("PaymentRequested fields are invalid");
        }
        PaymentProvider provider = providers.resolve(request.provider());
        CheckoutRules rules = provider.rules();
        if (!rules.supportsCurrency(request.currency()) || request.amountMinor() > rules.maxAmountMinor()) {
            throw new PoisonMessageException("PaymentRequested có amount/currency không hỗ trợ");
        }
        Map<String, String> options = options(request);
        byte[] hash = Hashes.sha256(provider.code() + "|" + request.bookingId() + "|" + request.reservationId()
                + "|" + request.amountMinor() + "|" + request.currency() + "|" + options);

        PaymentEntity payment = paymentRepository.findById(request.paymentId()).orElse(null);
        if (payment != null) {
            if (!sameRequest(payment, request, hash)) {
                throw new PoisonMessageException("PaymentRequested conflicts with the existing payment");
            }
            if (!PAY_REQUESTED.equals(payment.getStatus()) && !PAY_PROVIDER_PENDING.equals(payment.getStatus())) {
                return;
            }
            PaymentAttemptEntity existingAttempt = attemptRepository
                    .findFirstByPaymentIdOrderByCreatedAtDesc(payment.getId()).orElse(null);
            if (existingAttempt != null && existingAttempt.getCheckoutUrl() != null) {
                return;
            }
        } else {
            var byKey = paymentRepository.findByUserIdAndRequestKey(request.userId(), request.idempotencyKey());
            if (byKey.isPresent()) {
                throw new PoisonMessageException("PaymentRequested reused an idempotency key for another payment");
            }
            var byBooking = paymentRepository.findByBookingId(request.bookingId());
            if (byBooking.isPresent()) {
                throw new PoisonMessageException("Booking already has a different payment");
            }

            Instant now = Instant.now();
            payment = new PaymentEntity();
            payment.setId(request.paymentId());
            payment.setBookingId(request.bookingId());
            payment.setReservationId(request.reservationId());
            payment.setUserId(request.userId());
            payment.setAmountMinor(request.amountMinor());
            payment.setCurrency(request.currency());
            payment.setRequestKey(request.idempotencyKey());
            payment.setRequestHash(hash);
            payment.setStatus(PAY_REQUESTED);
            payment.setCreatedAt(now);
            payment.setUpdatedAt(now);
            paymentRepository.saveAndFlush(payment);
        }

        Instant now = Instant.now();
        PaymentAttemptEntity attempt = attemptRepository.findFirstByPaymentIdOrderByCreatedAtDesc(payment.getId())
                .orElse(null);
        if (attempt == null) {
            attempt = new PaymentAttemptEntity();
            attempt.setId(UUID.randomUUID());
            attempt.setPaymentId(payment.getId());
            attempt.setProvider(provider.code());
            attempt.setMerchantReference(attempt.getId().toString().replace("-", ""));
            attempt.setStatus(ATTEMPT_REQUESTED);
            attempt.setCreatedAt(now.truncatedTo(ChronoUnit.SECONDS));
            attempt.setUpdatedAt(now);
            attemptRepository.save(attempt);
        }
        if (!provider.code().equals(attempt.getProvider())) {
            throw new PoisonMessageException("PaymentRequested provider does not match its existing attempt");
        }
        Instant createdAt = attempt.getCreatedAt();
        Instant expireAt = earliest(request.holdExpiresAt().minus(rules.holdSafetyMargin()),
                createdAt.plus(rules.paymentWindow()));
        if (!expireAt.isAfter(now.plus(rules.minHoldRemaining()))) {
            attempt.setStatus(ATTEMPT_FAILED);
            attempt.setFailureCode("HOLD_EXPIRED");
            attempt.setUpdatedAt(now);
            payment.setStatus(PAY_FAILED);
            payment.setUpdatedAt(now);
            outboxService.record(EventTypes.PAYMENT_FAILED, 1, payment.getId(),
                    new PaymentFailedPayload(payment.getId(), payment.getBookingId(), payment.getReservationId(),
                            provider.code(), "HOLD_EXPIRED", now));
            return;
        }
        String clientIp = request.clientIp() == null || request.clientIp().isBlank()
                ? "127.0.0.1"
                : request.clientIp();
        String description = "Thanh toan ve xem phim " + request.bookingId().toString().substring(0, 8);
        attempt.setCheckoutUrl(provider.createCheckout(new CheckoutRequest(
                attempt.getMerchantReference(), request.amountMinor(), request.currency(), description, clientIp,
                createdAt, expireAt, options)).url());
        attempt.setStatus(ATTEMPT_PROVIDER_PENDING);
        attempt.setFailureCode(null);
        attempt.setUpdatedAt(now);
        attemptRepository.save(attempt);
        payment.setStatus(PAY_PROVIDER_PENDING);
        payment.setUpdatedAt(now);
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

    private static Map<String, String> options(PaymentRequestedPayload request) {
        Map<String, String> options = new TreeMap<>();
        if (request.bankCode() != null && !request.bankCode().isBlank()) {
            options.put("bankCode", request.bankCode());
        }
        if (request.locale() != null && !request.locale().isBlank()) {
            options.put("locale", request.locale());
        }
        return options;
    }

    private static boolean sameRequest(PaymentEntity payment, PaymentRequestedPayload request, byte[] hash) {
        return payment.getBookingId().equals(request.bookingId())
                && payment.getReservationId().equals(request.reservationId())
                && payment.getUserId().equals(request.userId())
                && payment.getAmountMinor() == request.amountMinor()
                && payment.getCurrency().equals(request.currency())
                && payment.getRequestKey().equals(request.idempotencyKey())
                && Arrays.equals(payment.getRequestHash(), hash);
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

        return new PaymentResponse(p.getId(), p.getBookingId(), p.getReservationId(), p.getStatus(),
                p.getAmountMinor(), p.getCurrency(), checkout, failureCode, p.getCreatedAt(), p.getUpdatedAt());
    }

    private RefundResponse toRefundResponse(RefundEntity r) {
        return new RefundResponse(r.getId(), r.getPaymentId(), r.getStatus(), r.getAmountMinor(), r.getCurrency(),
                r.getFailureCode(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy payment");
    }
}
