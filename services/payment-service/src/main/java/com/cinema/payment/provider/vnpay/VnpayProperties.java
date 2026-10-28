package com.cinema.payment.provider.vnpay;

import java.time.Duration;

/**
 * Cấu hình VNPay, bind từ payment.vnpay.* tại PaymentConfiguration.
 * Class thuần Java, không phụ thuộc Spring.
 */
public class VnpayProperties {
    private String tmnCode = "";
    private String hashSecret = "";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private String returnUrl = "";
    private String version = "2.1.0";
    private String orderType = "other";
    private String defaultLocale = "vn";
    /** Thời gian tối đa khách được thanh toán kể từ lúc tạo URL. */
    private Duration paymentWindow = Duration.ofMinutes(15);
    /** URL thanh toán hết hạn sớm hơn hold của reservation một khoảng này. */
    private Duration holdSafetyMargin = Duration.ofSeconds(15);
    /** Không tạo thanh toán nếu hold còn ít hơn khoảng này. */
    private Duration minHoldRemaining = Duration.ofSeconds(90);
    private Duration apiTimeout = Duration.ofSeconds(10);
    /** vnp_IpAddr cho querydr/refund (IP máy chủ gọi API). */
    private String serverIp = "127.0.0.1";
    private String createBy = "payment-service";
    /** Kiểm tra checksum phản hồi của querydr/refund. */
    private boolean verifyApiSignature = true;

    public void validate() {
        if (tmnCode == null || tmnCode.isBlank() || hashSecret == null || hashSecret.isBlank()) {
            throw new IllegalStateException(
                    "Thiếu cấu hình VNPay: đặt VNPAY_TMN_CODE và VNPAY_HASH_SECRET "
                            + "(payment.vnpay.tmn-code / payment.vnpay.hash-secret)");
        }
        if (returnUrl == null || returnUrl.isBlank()) {
            throw new IllegalStateException("Thiếu payment.vnpay.return-url");
        }
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public void setTmnCode(String v) {
        this.tmnCode = v;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String v) {
        this.hashSecret = v;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public void setPayUrl(String v) {
        this.payUrl = v;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String v) {
        this.apiUrl = v;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String v) {
        this.returnUrl = v;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String v) {
        this.version = v;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String v) {
        this.orderType = v;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String v) {
        this.defaultLocale = v;
    }

    public Duration getPaymentWindow() {
        return paymentWindow;
    }

    public void setPaymentWindow(Duration v) {
        this.paymentWindow = v;
    }

    public Duration getHoldSafetyMargin() {
        return holdSafetyMargin;
    }

    public void setHoldSafetyMargin(Duration v) {
        this.holdSafetyMargin = v;
    }

    public Duration getMinHoldRemaining() {
        return minHoldRemaining;
    }

    public void setMinHoldRemaining(Duration v) {
        this.minHoldRemaining = v;
    }

    public Duration getApiTimeout() {
        return apiTimeout;
    }

    public void setApiTimeout(Duration v) {
        this.apiTimeout = v;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String v) {
        this.serverIp = v;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String v) {
        this.createBy = v;
    }

    public boolean isVerifyApiSignature() {
        return verifyApiSignature;
    }

    public void setVerifyApiSignature(boolean v) {
        this.verifyApiSignature = v;
    }
}
