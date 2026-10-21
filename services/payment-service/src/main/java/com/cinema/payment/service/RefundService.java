package com.cinema.payment.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.cinema.payment.entity.PaymentAttemptEntity;
import com.cinema.payment.entity.PaymentEntity;
import com.cinema.payment.entity.RefundEntity;
import com.cinema.payment.messaging.EventTypes;
import com.cinema.payment.messaging.PoisonMessageException;
import com.cinema.payment.provider.vnpay.VnpayApiException;
import com.cinema.payment.provider.vnpay.VnpayCodec;
import com.cinema.payment.provider.vnpay.VnpayGateway;
import com.cinema.payment.provider.vnpay.VnpayRefundResult;
import com.cinema.payment.repository.PaymentAttemptRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.RefundRepository;

import static com.cinema.payment.entity.Statuses.*;

/**
 * Hoàn tiền toàn phần. Hai bước tách biệt:
 * 1) requestRefund: ghi refund REQUESTED + payment REFUND_PENDING trong
 * transaction của event (không gọi HTTP);
 * 2) processDue: job gọi VNPay ngoài transaction, có lease để nhiều instance
 * không gọi trùng, và thử lại khi
 * kết quả chưa chắc chắn (timeout, 99...) cho tới max-age.
 */
@Service
public class RefundService {
    private static final Logger LOG = Logger.getLogger(RefundService.class.getName());

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final RefundRepository refundRepository;
    private final VnpayGateway gateway;
    private final OutboxService outboxService;
    private final TransactionTemplate tx;
    private final Duration retryDelay;
    private final Duration maxAge;

    public RefundService(PaymentRepository paymentRepository,
            PaymentAttemptRepository attemptRepository,
            RefundRepository refundRepository,
            VnpayGateway gateway,
            OutboxService outboxService,
            TransactionTemplate tx,
            @Value("${payment.refund.retry-delay:PT30S}") Duration retryDelay,
            @Value("${payment.refund.max-age:PT24H}") Duration maxAge) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.refundRepository = refundRepository;
        this.gateway = gateway;
        this.outboxService = outboxService;
        this.tx = tx;
        this.retryDelay = retryDelay;
        this.maxAge = maxAge;
    }

    // Idempotent: mỗi payment tối đa một refund. Gọi trong transaction của caller
    @Transactional
    public void requestRefund(UUID paymentId, String reason) {
        PaymentEntity payment = paymentRepository.lockById(paymentId)
                .orElseThrow(() -> new PoisonMessageException("Hoàn tiền cho payment không tồn tại: " + paymentId));
        if (refundRepository.findByPaymentId(paymentId).isPresent()) {
            return;
        }

        if (!PAY_SUCCEEDED.equals(payment.getStatus())) {
            LOG.warning("Bỏ qua yêu cầu hoàn tiền: payment " + paymentId + " đang ở trạng thái "
                    + payment.getStatus() + " (lý do: " + reason + ")");
            return;
        }
        if (attemptRepository.findFirstByPaymentIdAndStatusOrderByCreatedAtDesc(paymentId, ATTEMPT_SUCCEEDED)
                .isEmpty()) {
            throw new PoisonMessageException("Payment " + paymentId + " SUCCEEDED nhưng không có attempt thành công");
        }
        Instant now = Instant.now();
        UUID requestId = UUID.randomUUID();
        String requestKey = "refund:" + paymentId;
        RefundEntity refund = new RefundEntity();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(paymentId);
        refund.setRefundRequestId(requestId);
        refund.setAmountMinor(payment.getAmountMinor());
        refund.setCurrency(payment.getCurrency());
        refund.setRequestKey(requestKey);
        refund.setRequestHash(VnpayCodec.sha256(requestKey + "|" + reason));
        refund.setProvider(PROVIDER_VNPAY);
        refund.setMerchantReference(requestId.toString().replace("-", "")); // vnp_RequestId, 32 ký tự
        refund.setStatus(REFUND_REQUESTED);
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        refundRepository.saveAndFlush(refund);

        payment.setStatus(PAY_REFUND_PENDING);
        payment.setUpdatedAt(now);
        LOG.info("Đã tạo yêu cầu hoàn tiền cho payment " + paymentId + " (lý do: " + reason + ")");
    }

    /** Xử lý các refund đến hạn. Gọi định kỳ bởi RefundDispatchJob. */
    public void processDue(int limit) {
        Instant now = Instant.now();
        for (RefundEntity r : refundRepository.findDue(now, now.minus(retryDelay), PageRequest.of(0, limit))) {
            try {
                dispatch(r);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Xử lý refund " + r.getId() + " lỗi", e);
            }
        }
    }

    private void dispatch(RefundEntity r) {
        Instant seen = r.getUpdatedAt();
        Integer claimed = tx.execute(s -> refundRepository.claim(r.getId(), seen, Instant.now()));
        if (claimed == null || claimed == 0) {
            return; // instance khác đã nhận
        }

        PaymentAttemptEntity attempt = attemptRepository
                .findFirstByPaymentIdAndStatusOrderByCreatedAtDesc(r.getPaymentId(), ATTEMPT_SUCCEEDED).orElse(null);
        if (attempt == null) {
            finish(r.getId(), false, "NO_SUCCEEDED_ATTEMPT", null);
            return;
        }
        if (r.getCreatedAt().plus(maxAge).isBefore(Instant.now())) {
            LOG.severe("Refund " + r.getId() + " không xác định được kết quả sau " + maxAge + "; cần xử lý thủ công");
            finish(r.getId(), false, "UNRESOLVED", null);
            return;
        }
        VnpayRefundResult res;
        try {
            res = gateway.refund(new VnpayGateway.RefundParams(
                    r.getMerchantReference(), attempt.getMerchantReference(), r.getAmountMinor(),
                    attempt.getProviderTransactionId(), attempt.getCreatedAt(),
                    "Hoan tien ve xem phim " + r.getPaymentId().toString().substring(0, 8)));
        } catch (VnpayApiException e) {
            LOG.log(Level.WARNING, "Gọi refund VNPay lỗi (kết quả chưa rõ), sẽ thử lại: " + r.getId(), e);
            return;
        }
        if (!res.signatureValid()) {
            LOG.warning("Phản hồi refund có chữ ký không hợp lệ (refund " + r.getId() + "); sẽ thử lại");
            return;
        }
        String code = res.responseCode() == null ? "" : res.responseCode();
        switch (code) {
            case "00":
                if ("09".equals(res.transactionStatus())) { // GD hoàn trả bị từ chối
                    finish(r.getId(), false, "VNPAY_TS_09", null);
                } else {
                    finish(r.getId(), true, null, res.transactionNo());
                }
                break;
            case "94": // đã gửi yêu cầu hoàn trước đó, VNPay đang xử lý => coi như đã được chấp nhận
                finish(r.getId(), true, null, null);
                break;
            case "91":
            case "95":
            case "03":
                finish(r.getId(), false, "VNPAY_" + code, null);
                break;
            default: // 02 (TmnCode), 97 (checksum), 99 (lỗi khác)...: có thể do cấu hình/tạm thời,
                     // thử lại
                LOG.warning("Refund " + r.getId() + " nhận mã " + code + " (" + res.message() + "); sẽ thử lại");
        }

    }

    private void finish(UUID refundId, boolean completed, String failureCode, String providerRefundId) {
        tx.executeWithoutResult(s -> {
            RefundEntity refund = refundRepository.findById(refundId).orElseThrow();
            if (REFUND_COMPLETED.equals(refund.getStatus()) || REFUND_FAILED.equals(refund.getStatus())) {
                return;
            }
            PaymentEntity payment = paymentRepository.lockById(refund.getPaymentId()).orElseThrow();
            Instant now = Instant.now();
            refund.setUpdatedAt(now);
            payment.setUpdatedAt(now);
            if (completed) {
                refund.setStatus(REFUND_COMPLETED);
                refund.setProviderRefundId(providerRefundId);
                payment.setStatus(PAY_REFUNDED);
                outboxService.record(EventTypes.PAYMENT_REFUNDED, 1, payment.getId(),
                        PaymentSettlementService.payload(payment, providerRefundId, null, "vnpay"));
            } else {
                refund.setStatus(REFUND_FAILED);
                refund.setFailureCode(failureCode);
                payment.setStatus(PAY_REFUND_FAILED);
                outboxService.record(EventTypes.PAYMENT_REFUND_FAILED, 1, payment.getId(),
                        PaymentSettlementService.payload(payment, null, failureCode, "vnpay"));
            }
        });
    }
}
