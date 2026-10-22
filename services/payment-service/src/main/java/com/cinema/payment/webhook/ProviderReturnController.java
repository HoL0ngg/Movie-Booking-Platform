package com.cinema.payment.webhook;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.exception.ApiException;
import com.cinema.payment.service.PaymentReconciliationService;
import com.cinema.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/provider-return/{provider}")
public class ProviderReturnController {
    private static final Logger LOG = Logger.getLogger(ProviderReturnController.class.getName());

    private final Map<String, PaymentReturnHandler> handlers;
    private final PaymentReconciliationService reconciliation;
    private final PaymentService paymentService;
    private final String frontendResultUrl;

    public ProviderReturnController(
            List<PaymentReturnHandler> handlerList,
            PaymentReconciliationService reconciliation,
            PaymentService paymentService,
            @Value("${payment.frontend-result-url}") String frontendResultUrl) {
        // Spring tự inject các handler: VNPay, MoMo, ...
        this.handlers = handlerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PaymentReturnHandler::provider,
                        Function.identity()));

        this.reconciliation = reconciliation;
        this.paymentService = paymentService;
        this.frontendResultUrl = frontendResultUrl;
    }

    @GetMapping
    public ResponseEntity<Void> providerReturn(@PathVariable String provider,
            HttpServletRequest request) {
        // Tìm handler theo provider trên URL.
        PaymentReturnHandler handler = handlers.get(provider);

        if (handler == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "UNKNOWN_PROVIDER",
                    "Provider không được hỗ trợ");
        }
        UriComponentsBuilder target = UriComponentsBuilder.fromUriString(frontendResultUrl);

        // Phân tích return URL theo định dạng của provider tương ứng.
        ParsedPaymentReturn notification;

        try {
            notification = handler.parseReturn(request);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không thể phân tích provider return", e);

            target.queryParam("status", "INVALID_SIGNATURE");
            return redirect(target);
        }

        // Không tin dữ liệu trả về nếu chữ ký hoặc thông tin xác thực sai.
        if (!notification.signatureValid()
                || notification.transactionRef() == null) {
            target.queryParam("status", "INVALID_SIGNATURE");
            return redirect(target);
        }

        String txnRef = notification.transactionRef();
        // Cố gắng đối soát nhưng không chặn việc đưa người dùng về frontend
        try {
            reconciliation.reconcileByTxnRef(txnRef);
        } catch (Exception e) {
            LOG.log(
                    Level.WARNING,
                    "Đối soát khi quay về từ provider lỗi",
                    e);
        }

        // Lấy trạng thái thanh toán từ hệ thống
        // không lấy response code làm kết luận thanh toán thành công.

        PaymentResponse paymentResponse = paymentService.findByMerchantReference(provider, txnRef);
        if (paymentResponse == null) {
            target.queryParam("status", "NOT_FOUND");
        } else {
            target.queryParam("paymentId", paymentResponse.paymentId())
                    .queryParam("status", paymentResponse.status())
                    .queryParam(
                            "providerCode",
                            notification.providerCode());
        }
        return redirect(target);
    }

    private static ResponseEntity<Void> redirect(
            UriComponentsBuilder target) {
        URI uri = target.build().encode().toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, uri.toString())
                .build();
    }
}
