package com.cinema.payment.service;

import static com.cinema.payment.entity.Statuses.*;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.payment.entity.PaymentAttemptEntity;
import com.cinema.payment.entity.PaymentEntity;
import com.cinema.payment.entity.ProviderResult;
import com.cinema.payment.enums.Result;
import com.cinema.payment.messaging.EventTypes;
import com.cinema.payment.messaging.PaymentPayload;
import com.cinema.payment.provider.vnpay.VnpayCodec;
import com.cinema.payment.repository.PaymentAttemptRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.ProviderEventRepository;

@Service
public class PaymentSettlementService {
    private static final Logger LOG = Logger.getLogger(PaymentSettlementService.class.getName());

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final ProviderEventRepository providerEventRepository;
    private final OutboxService outboxService;

    public PaymentSettlementService(PaymentRepository paymentRepository,
            PaymentAttemptRepository attemptRepository,
            ProviderEventRepository providerEventRepository,
            OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.providerEventRepository = providerEventRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public Result settle(ProviderResult r, String provider) {
        PaymentAttemptEntity attempt = attemptRepository.findByProviderAndMerchantReference(provider, r.txnRef())
                .orElse(null);
        if (attempt == null) {

            return Result.NOT_FOUND;
        }

        PaymentEntity paymentEntity = paymentRepository.lockById(attempt.getPaymentId()).orElseThrow();
        if (r.amountVnp() != null && r.amountVnp() != Math.multiplyExact(paymentEntity.getAmountMinor(), 100L)) {
            LOG.warning("Sai số tiền cho payment " + paymentEntity.getId() + ": VNPay=" + r.amountVnp()
                    + ", mong đợi=" + paymentEntity.getAmountMinor() * 100);
            return Result.AMOUNT_MISMATCH;
        }

        Instant now = Instant.now();
        int inserted = providerEventRepository.insertIfAbsent(UUID.randomUUID(), paymentEntity.getId(), provider,
                r.eventIdentity(), r.transactionNo(), VnpayCodec.sha256(r.eventIdentity()), r.outcome().name(), now);
        if (inserted == 0) {
            return Result.ALREADY_PROCESSED;
        }
        switch (r.outcome()) {
            case PENDING:
                return Result.ACKNOWLEDGED;
            case SUCCEEDED:
                return applySuccess(paymentEntity, attempt, r, now);

            default:
                return applyFailure(paymentEntity, attempt, r, now);
        }
    }

    private Result applySuccess(PaymentEntity paymentEntity, PaymentAttemptEntity paymentAttemptEntity,
            ProviderResult r,
            Instant now) {
        String status = paymentEntity.getStatus();
        boolean open = PAY_REQUESTED.equals(status) || PAY_PROVIDER_PENDING.equals(status);
        boolean lateSuccess = PAY_FAILED.equals(status); // đã đánh dấu thất bại/hết hạn nhưng thực tế khách đã trả tiền

        if (!open && !lateSuccess) {
            return Result.ALREADY_PROCESSED;
        }
        if (lateSuccess) {
            LOG.warning("Thanh toán thành công đến muộn cho payment " + paymentEntity.getId()
                    + " đã FAILED; phát PaymentSucceeded để booking xác nhận hoặc bù trừ bằng hoàn tiền");
        }

        paymentAttemptEntity.setStatus(ATTEMPT_SUCCEEDED);
        paymentAttemptEntity.setFailureCode(null);
        if (r.transactionNo() != null && !r.transactionNo().isBlank() && !"0".equals(r.transactionNo())) {
            paymentAttemptEntity.setProviderTransactionId(r.transactionNo());
        }
        paymentAttemptEntity.setUpdatedAt(now);
        paymentEntity.setStatus(PAY_SUCCEEDED);
        paymentEntity.setUpdatedAt(now);
        outboxService.record(EventTypes.PAYMENT_SUCCEEDED, 1, paymentEntity.getId(),
                payload(paymentEntity, null, r.failureCode(), PROVIDER_VNPAY));

        return Result.SETTLED;
    }

    private Result applyFailure(PaymentEntity paymentEntity, PaymentAttemptEntity attempt, ProviderResult r,
            Instant now) {
        String status = paymentEntity.getStatus();
        if (!PAY_REQUESTED.equals(status) && !PAY_PROVIDER_PENDING.equals(status)) {
            return Result.ALREADY_PROCESSED;
        }
        attempt.setStatus(ATTEMPT_FAILED);
        attempt.setFailureCode(r.failureCode());
        attempt.setUpdatedAt(now);
        paymentEntity.setStatus(PAY_FAILED);
        paymentEntity.setUpdatedAt(now);

        outboxService.record(EventTypes.PAYMENT_FAILED, 1, paymentEntity.getId(),
                payload(paymentEntity, null, r.failureCode(), PROVIDER_VNPAY));
        return Result.SETTLED;
    }

    static PaymentPayload payload(PaymentEntity p, String providerTransactionId, String reasonCode, String provider) {
        return new PaymentPayload(p.getId(), p.getReservationId(), p.getUserId(), p.getAmountMinor(), p.getCurrency(),
                provider, providerTransactionId, reasonCode);
    }
}
