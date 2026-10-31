package com.cinema.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cinema.payment.entity.PaymentAttemptEntity;
import com.cinema.payment.entity.ProviderResult;
import com.cinema.payment.enums.ProviderOutcome;
import com.cinema.payment.provider.vnpay.VnpayApiException;
import com.cinema.payment.provider.vnpay.VnpayGateway;
import com.cinema.payment.provider.vnpay.VnpayProperties;
import com.cinema.payment.provider.vnpay.VnpayQueryResult;
import com.cinema.payment.repository.PaymentAttemptRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cinema.payment.entity.Statuses.*;

/**
 * Đối soát chủ động bằng API querydr (server-to-server, có chữ ký). Dùng khi
 * IPN không tới được
 * (vd. môi trường local không có URL public) hoặc IPN bị lỡ. Không đặt trong
 * transaction vì có gọi HTTP.
 */
@Service
public class PaymentReconciliationService {
    private static final Logger LOG = Logger.getLogger(PaymentReconciliationService.class.getName());

    private final PaymentAttemptRepository attemptRepository;
    private final VnpayGateway gateway;
    private final PaymentSettlementService settlement;
    private final VnpayProperties props;
    private final Duration expiryGrace;

    public PaymentReconciliationService(PaymentAttemptRepository attemptRepository,
            VnpayGateway gateway,
            PaymentSettlementService settlement,
            VnpayProperties props,
            @Value("${payment.reconcile.expiry-grace:PT2M}") Duration expiryGrace) {
        this.attemptRepository = attemptRepository;
        this.gateway = gateway;
        this.settlement = settlement;
        this.props = props;
        this.expiryGrace = expiryGrace;
    }

    /**
     * Đối soát theo vnp_TxnRef (dùng khi khách quay về từ VNPay). Bỏ qua nếu không
     * tìm thấy.
     */
    public void reconcileByTxnRef(String txnRef) {
        attemptRepository.findByProviderAndMerchantReference(PROVIDER_VNPAY, txnRef)
                .ifPresent(a -> reconcile(a.getId()));
    }

    public void reconcile(UUID attemptId) {
        PaymentAttemptEntity attempt = attemptRepository.findById(attemptId).orElse(null);
        if (attempt == null || !ATTEMPT_PROVIDER_PENDING.equals(attempt.getStatus())) {
            return;
        }
        String txnRef = attempt.getMerchantReference();
        VnpayQueryResult q;
        try {
            q = gateway.queryTransaction(txnRef, attempt.getCreatedAt());
        } catch (VnpayApiException e) {
            // Không in stack trace: đây là lỗi lặp lại mỗi vòng đối soát, thông điệp đã đủ để chẩn đoán.
            LOG.log(Level.WARNING, "querydr lỗi cho {0}, thử lại ở vòng sau: {1}",
                    new Object[] { txnRef, e.getMessage() });
            return;
        }
        if (!q.signatureValid()) {
            LOG.warning("querydr trả chữ ký không hợp lệ cho " + txnRef + "; bỏ qua");
            return;
        }

        boolean pastDeadline = Instant.now()
                .isAfter(attempt.getCreatedAt().plus(props.getPaymentWindow()).plus(expiryGrace));

        if (q.notFound()) { // VNPay chưa ghi nhận giao dịch: khách chưa vào trang thanh toán / bỏ ngang
            if (pastDeadline) {
                expire(txnRef);
            }
            return;
        }
        if (!q.found()) {
            LOG.warning("querydr " + txnRef + " trả mã " + q.responseCode() + ": " + q.message());
            return;
        }
        if (q.transactionType() != null && !"01".equals(q.transactionType())) {
            return; // chỉ quan tâm giao dịch thanh toán, không phải hoàn tiền
        }

        ProviderOutcome outcome = q.outcome();
        if (outcome == ProviderOutcome.PENDING) {
            if ("01".equals(q.transactionStatus())) { // chưa hoàn tất
                if (pastDeadline) {
                    expire(txnRef);
                }
            } else { // 04 giao dịch đảo / 07 nghi gian lận: tiền có thể đã bị trừ, không tự động kết
                     // luận
                LOG.severe("Giao dịch " + txnRef + " ở trạng thái VNPay " + q.transactionStatus()
                        + " cần kiểm tra thủ công trên cổng quản trị VNPay");
            }
            return;
        }
        settlement.settle(new ProviderResult(txnRef, q.amount(), outcome, q.transactionNo(),
                "TS_" + q.transactionStatus(), q.eventIdentity()), PROVIDER_VNPAY);
    }

    private void expire(String txnRef) {
        settlement.settle(new ProviderResult(txnRef, null, ProviderOutcome.FAILED, null, "EXPIRED",
                txnRef + ":expired"), PROVIDER_VNPAY);
    }

}
