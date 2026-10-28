package com.cinema.payment.provider.vnpay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cinema.payment.enums.ProviderOutcome;

/**
 * Ký / kiểm tra chữ ký / định dạng thời gian theo đặc tả VNPay 2.1.0
 * (HMAC-SHA512).
 * Thuần Java, không trạng thái.
 */
public final class VnpayCodec {
    public static final String SECURE_HASH = "vnp_SecureHash";
    public static final String SECURE_HASH_TYPE = "vnp_SecureHashType";

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh"); // GMT+7
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VN_ZONE);
    /**
     * vnp_TransactionStatus chưa phải kết quả cuối: chưa hoàn tất, giao dịch đảo,
     * nghi ngờ gian lận.
     */
    private static final Set<String> PENDING_STATUSES = Set.of("01", "04", "07");

    private VnpayCodec() {
    }

    /** yyyyMMddHHmmss theo GMT+7. */
    public static String formatTime(Instant instant) {
        return VNP_TIME.format(instant);
    }

    public static String hmacSha512Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không tính được HMAC-SHA512", e);
        }
    }

    /**
     * Chuỗi cần ký: tham số sắp xếp theo tên, bỏ giá trị rỗng, dạng
     * key=urlencode(value) nối bằng dấu &amp;.
     * vnp_SecureHash / vnp_SecureHashType không bao giờ nằm trong chuỗi ký.
     */
    public static String signData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(params).entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (v == null || v.isEmpty() || SECURE_HASH.equals(k) || SECURE_HASH_TYPE.equals(k)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(k)).append('=').append(encode(v));
        }
        return sb.toString();
    }

    /** Query string đã ký, sẵn sàng nối vào pay URL. */
    public static String signedQuery(Map<String, String> params, String secret) {
        String data = signData(params);
        return data + "&" + SECURE_HASH + "=" + hmacSha512Hex(secret, data);
    }

    /** Kiểm tra vnp_SecureHash của dữ liệu VNPay gửi về (IPN / Return URL). */
    public static boolean verify(Map<String, String> params, String secret) {
        String received = params.get(SECURE_HASH);
        if (received == null || received.isBlank()) {
            return false;
        }
        return constantTimeEquals(hmacSha512Hex(secret, signData(params)), received.trim());
    }

    /**
     * Checksum kiểu API (querydr/refund): các trường nối bằng dấu |, trường thiếu
     * coi là rỗng.
     */
    public static String pipeHash(String secret, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(parts[i] == null ? "" : parts[i]);
        }
        return hmacSha512Hex(secret, sb.toString());
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.toLowerCase().getBytes(StandardCharsets.UTF_8),
                b.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Chuẩn hoá vnp_TransactionStatus: 00 thành công; 01/04/07 chưa chốt; còn lại
     * thất bại.
     */
    public static ProviderOutcome outcomeOfStatus(String transactionStatus) {
        if ("00".equals(transactionStatus)) {
            return ProviderOutcome.SUCCEEDED;
        }
        if (transactionStatus != null && PENDING_STATUSES.contains(transactionStatus)) {
            return ProviderOutcome.PENDING;
        }
        return ProviderOutcome.FAILED;
    }

    public static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
