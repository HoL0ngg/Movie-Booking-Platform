package com.cinema.payment.webhook;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.payment.exception.ApiException;
import com.cinema.payment.service.PaymentSettlementService;
import com.cinema.payment.enums.Result.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/provider-webhooks")
public class ProviderWebhookController {
    private static final Logger LOG = Logger.getLogger(ProviderWebhookController.class.getName());

    // Lưu các handler theo tên provider.
    private final Map<String, PaymentWebhookHandler> handlers;

    private final PaymentSettlementService settlement;

    public ProviderWebhookController(List<PaymentWebhookHandler> handlerList,
            PaymentSettlementService settlement) {
        // Spring tự inject tất cả implementation của PaymentWebhookHandler.
        // Ví dụ: VnpayWebhookHandler, MomoWebhookHandler.
        this.handlers = handlerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        PaymentWebhookHandler::provider,
                        Function.identity()));

        this.settlement = settlement;
    }

    @RequestMapping(value = "/{provider}", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<Map<String, String>> receive(@PathVariable String provider, HttpServletRequest request) {
        PaymentWebhookHandler handler = handlers.get(provider);
        if (handler == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "UNKNOWN_PROVIDER",
                    "Provider không được hỗ trợ");
        }

        try {

            // Handler tự phân tích request theo định dạng của provider.
            ParsedWebhook notification = handler.parseNotification(request);

            // Không xử lý thanh toán nếu chữ ký không hợp lệ.
            if (!notification.signatureValid()) {
                LOG.warning(
                        "Webhook có chữ ký hoặc thông tin xác thực không hợp lệ");
                return rsp("97", "Invalid signature");
            }

            // Không tìm được mã giao dịch nội bộ.
            if (notification.transactionRef() == null) {
                return rsp("01", "Order not found");
            }

            // Chuyển kết quả đã chuẩn hóa cho tầng nghiệp vụ.
            var result = settlement.settle(notification.result(), provider);

            // Ánh xạ kết quả nghiệp vụ sang response của webhook.
            return switch (result) {
                case SETTLED, ACKNOWLEDGED ->
                    rsp("00", "Confirm Success");

                case ALREADY_PROCESSED ->
                    rsp("02", "Order already confirmed");

                case NOT_FOUND ->
                    rsp("01", "Order not found");

                case AMOUNT_MISMATCH ->
                    rsp("04", "invalid amount");
            };
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Xử lý webhook thanh toán lỗi", e);
            return rsp("99", "Unknown error");
        }
    }

    private static ResponseEntity<Map<String, String>> rsp(String code, String message) {
        return ResponseEntity.ok(Map.of("RspCode", code, "Message", message));
    }
}
