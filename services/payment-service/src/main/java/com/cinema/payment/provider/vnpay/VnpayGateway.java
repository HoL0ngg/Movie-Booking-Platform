package com.cinema.payment.provider.vnpay;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.cinema.payment.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tích hợp VNPay Sandbox (vnp_Version 2.1.0): tạo URL thanh toán, xác thực
 * IPN/Return,
 * truy vấn (querydr) và hoàn tiền (refund). Không phụ thuộc Spring; được tạo
 * bean ở PaymentConfiguration.
 */
public class VnpayGateway {

    /** Tham số tạo URL thanh toán. amountMinor là VND (chưa nhân 100). */
    public record CheckoutParams(
            String txnRef,
            long amountMinor,
            String orderInfo,
            String ipAddr,
            String locale,
            String bankCode,
            Instant createdAt,
            Instant expireAt) {
    }

    /** Tham số hoàn tiền toàn phần. amountMinor là VND (chưa nhân 100). */
    public record RefundParams(
            String requestId,
            String txnRef,
            long amountMinor,
            String transactionNo,
            Instant transactionCreatedAt,
            String orderInfo) {
    }

    private final VnpayProperties props;
    private final HttpClient http;

    public VnpayGateway(VnpayProperties props, HttpClient http) {
        this.props = props;
        this.http = http;
    }

    // ------------------------------------------------------------------ tạo URL
    // thanh toán

    public String buildCheckoutUrl(CheckoutParams p) {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("vnp_Version", props.getVersion());
        q.put("vnp_Command", "pay");
        q.put("vnp_TmnCode", props.getTmnCode());
        q.put("vnp_Amount", String.valueOf(Math.multiplyExact(p.amountMinor(), 100L)));
        q.put("vnp_CurrCode", "VND");
        q.put("vnp_TxnRef", p.txnRef());
        q.put("vnp_OrderInfo", p.orderInfo());
        q.put("vnp_OrderType", props.getOrderType());
        q.put("vnp_Locale", p.locale() == null ? props.getDefaultLocale() : p.locale());
        q.put("vnp_ReturnUrl", props.getReturnUrl());
        q.put("vnp_IpAddr", p.ipAddr());
        q.put("vnp_CreateDate", VnpayCodec.formatTime(p.createdAt()));
        q.put("vnp_ExpireDate", VnpayCodec.formatTime(p.expireAt()));
        if (p.bankCode() != null && !p.bankCode().isBlank()) {
            q.put("vnp_BankCode", p.bankCode());
        }
        return props.getPayUrl() + "?" + VnpayCodec.signedQuery(q, props.getHashSecret());
    }

    // ------------------------------------------------------------------ IPN /
    // Return URL

    /**
     * Phân tích tham số vnp_* và kiểm tra chữ ký. Không ném lỗi: dùng
     * signatureValid để quyết định.
     */
    public VnpayNotification parseNotification(Map<String, String> rawParams) {
        Map<String, String> params = new LinkedHashMap<>();
        rawParams.forEach((k, v) -> {
            if (k != null && k.startsWith("vnp_") && v != null) {
                params.put(k, v);
            }
        });
        boolean valid = VnpayCodec.verify(params, props.getHashSecret())
                && props.getTmnCode().equals(params.get("vnp_TmnCode"));
        return new VnpayNotification(
                valid,
                params.get("vnp_TmnCode"),
                params.get("vnp_TxnRef"),
                parseLong(params.get("vnp_Amount")),
                params.get("vnp_TransactionNo"),
                params.get("vnp_ResponseCode"),
                params.get("vnp_TransactionStatus"),
                params.get("vnp_PayDate"),
                params.get("vnp_BankCode"),
                params);
    }

    // ------------------------------------------------------------------ querydr

    public VnpayQueryResult queryTransaction(String txnRef, Instant transactionCreatedAt) {
        Instant now = Instant.now();
        String requestId = newRequestId();
        String createDate = VnpayCodec.formatTime(now);
        String txnDate = VnpayCodec.formatTime(transactionCreatedAt);
        String orderInfo = "Truy van giao dich " + txnRef;

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", props.getVersion());
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", props.getTmnCode());
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", txnDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", props.getServerIp());
        body.put("vnp_SecureHash", VnpayCodec.pipeHash(props.getHashSecret(),
                requestId, props.getVersion(), "querydr", props.getTmnCode(), txnRef,
                txnDate, createDate, props.getServerIp(), orderInfo));

        JsonNode r = post(body);
        boolean valid = !props.isVerifyApiSignature() || VnpayCodec.constantTimeEquals(
                VnpayCodec.pipeHash(props.getHashSecret(),
                        text(r, "vnp_ResponseId"), text(r, "vnp_Command"), text(r, "vnp_ResponseCode"),
                        text(r, "vnp_Message"), text(r, "vnp_TmnCode"), text(r, "vnp_TxnRef"),
                        text(r, "vnp_Amount"), text(r, "vnp_BankCode"), text(r, "vnp_PayDate"),
                        text(r, "vnp_TransactionNo"), text(r, "vnp_TransactionType"),
                        text(r, "vnp_TransactionStatus"), text(r, "vnp_OrderInfo"),
                        text(r, "vnp_PromotionCode"), text(r, "vnp_PromotionAmount")),
                text(r, "vnp_SecureHash"));
        return new VnpayQueryResult(
                valid,
                text(r, "vnp_ResponseCode"),
                text(r, "vnp_Message"),
                text(r, "vnp_TxnRef"),
                parseLong(text(r, "vnp_Amount")),
                text(r, "vnp_TransactionNo"),
                text(r, "vnp_TransactionType"),
                text(r, "vnp_TransactionStatus"),
                text(r, "vnp_PayDate"),
                text(r, "vnp_BankCode"));
    }

    // ------------------------------------------------------------------ refund

    public VnpayRefundResult refund(RefundParams p) {
        Instant now = Instant.now();
        String amount = String.valueOf(Math.multiplyExact(p.amountMinor(), 100L));
        String createDate = VnpayCodec.formatTime(now);
        String txnDate = VnpayCodec.formatTime(p.transactionCreatedAt());
        String transactionNo = p.transactionNo() == null ? "" : p.transactionNo();

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", p.requestId());
        body.put("vnp_Version", props.getVersion());
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", props.getTmnCode());
        body.put("vnp_TransactionType", "02"); // hoàn trả toàn phần
        body.put("vnp_TxnRef", p.txnRef());
        body.put("vnp_Amount", amount);
        body.put("vnp_OrderInfo", p.orderInfo());
        body.put("vnp_TransactionNo", transactionNo);
        body.put("vnp_TransactionDate", txnDate);
        body.put("vnp_CreateBy", props.getCreateBy());
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", props.getServerIp());
        body.put("vnp_SecureHash", VnpayCodec.pipeHash(props.getHashSecret(),
                p.requestId(), props.getVersion(), "refund", props.getTmnCode(), "02", p.txnRef(),
                amount, transactionNo, txnDate, props.getCreateBy(), createDate,
                props.getServerIp(), p.orderInfo()));

        JsonNode r = post(body);
        boolean valid = !props.isVerifyApiSignature() || VnpayCodec.constantTimeEquals(
                VnpayCodec.pipeHash(props.getHashSecret(),
                        text(r, "vnp_ResponseId"), text(r, "vnp_Command"), text(r, "vnp_ResponseCode"),
                        text(r, "vnp_Message"), text(r, "vnp_TmnCode"), text(r, "vnp_TxnRef"),
                        text(r, "vnp_Amount"), text(r, "vnp_BankCode"), text(r, "vnp_PayDate"),
                        text(r, "vnp_TransactionNo"), text(r, "vnp_TransactionType"),
                        text(r, "vnp_TransactionStatus"), text(r, "vnp_OrderInfo")),
                text(r, "vnp_SecureHash"));
        return new VnpayRefundResult(
                valid,
                text(r, "vnp_ResponseCode"),
                text(r, "vnp_Message"),
                text(r, "vnp_TransactionNo"),
                text(r, "vnp_TransactionStatus"),
                parseLong(text(r, "vnp_Amount")));
    }

    // ------------------------------------------------------------------ nội bộ

    private JsonNode post(Map<String, String> body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(props.getApiUrl()))
                .timeout(props.getApiTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new VnpayApiException("VNPay API trả HTTP " + response.statusCode());
            }
            return JsonUtils.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VnpayApiException("Bị ngắt khi gọi VNPay API", e);
        } catch (java.io.IOException | IllegalStateException e) {
            throw new VnpayApiException("Không gọi/đọc được VNPay API: " + e.getMessage(), e);
        }
    }

    private static String newRequestId() {
        return UUID.randomUUID().toString().replace("-", ""); // 32 ký tự, đúng giới hạn [1,32]
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static long parseLong(String s) {
        try {
            return s == null ? -1 : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
