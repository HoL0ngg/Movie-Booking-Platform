package com.cinema.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cinema.auth.dto.OtpChallengeResponse;
import com.cinema.auth.exception.AuthException;

@Service
public class OtpService {

    public enum Purpose { REGISTER, LOGIN }

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;
    private final TokenHasher tokenHasher;
    private final OtpMailSender mailSender;
    private final Duration ttl;
    private final Duration cooldown;
    private final int maxAttempts;
    private final long mailWaitMs;
    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "otp-mail");
        t.setDaemon(true);
        return t;
    });

    public OtpService(StringRedisTemplate redis, TokenHasher tokenHasher, OtpMailSender mailSender,
                      @Value("${auth.otp.ttl:5m}") Duration ttl,
                      @Value("${auth.otp.resend-cooldown:60s}") Duration cooldown,
                      @Value("${auth.otp.max-attempts:5}") int maxAttempts,
                      @Value("${auth.otp.mail-wait:3500ms}") Duration mailWait) {
        this.redis = redis;
        this.tokenHasher = tokenHasher;
        this.mailSender = mailSender;
        this.ttl = ttl;
        this.cooldown = cooldown;
        this.maxAttempts = maxAttempts;
        this.mailWaitMs = mailWait.toMillis(); // phải < response-timeout của gateway (5s)
    }

    /** Sinh OTP, lưu hash vào Redis, gửi mail. payload = dữ liệu chờ (hash mật khẩu khi đăng ký), có thể null. */
    public OtpChallengeResponse issue(Purpose purpose, String email, String payload) {
        String key = key(purpose, email);
        Boolean first;
        try {
            first = redis.opsForValue().setIfAbsent(key + ":cooldown", "1", cooldown); // atomic: chống spam
        } catch (RuntimeException ex) {
            log.error("Redis lỗi khi tạo cooldown OTP cho {}", email, ex);
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "OTP_STORE_UNAVAILABLE",
                    "Verification service is temporarily unavailable.");
        }
        if (!Boolean.TRUE.equals(first)) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_TOO_SOON",
                    "Please wait before requesting another code.");
        }
        String otp = "%06d".formatted(random.nextInt(1_000_000)); // 000000–999999
        String codeHash = hash(email, otp);
        try {
            redis.opsForValue().set(key + ":code", codeHash, ttl);
            redis.delete(key + ":tries"); // mã mới → đếm lại số lần sai
            if (payload != null) redis.opsForValue().set(key + ":payload", payload, ttl);
        } catch (RuntimeException ex) { // bất kỳ lỗi nào sau khi đặt cooldown đều phải nhả cooldown
            log.error("Redis lỗi khi lưu OTP cho {}", email, ex);
            release(key, null);
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "OTP_STORE_UNAVAILABLE",
                    "Verification service is temporarily unavailable.");
        }

        // Gửi mail ở thread riêng, chỉ chờ tối đa mailWaitMs để không vượt response-timeout của gateway (→ 504).
        CompletableFuture<Void> sending = CompletableFuture.runAsync(() -> mailSender.send(email, otp, ttl), mailExecutor);
        sending.whenComplete((ok, err) -> { // gửi chậm rồi mới lỗi → vẫn dọn để user thử lại ngay
            if (err != null) { // release() idempotent nên trùng với nhánh bắt lỗi bên dưới cũng không sao
                release(key, codeHash);
            }
        });
        try {
            sending.get(mailWaitMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            // SMTP chậm: mail có thể vẫn đến. Giữ mã + cooldown, trả thành công; nếu sau đó lỗi sẽ được dọn ở whenComplete.
            log.warn("Gửi OTP tới {} chưa xong sau {}ms, tiếp tục ở nền", email, mailWaitMs);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.error("Gửi OTP tới {} thất bại", email, cause);
            release(key, codeHash);
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED",
                    "Could not send the verification email.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            release(key, codeHash);
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED",
                    "Could not send the verification email.");
        }
        return new OtpChallengeResponse(ttl.toSeconds(), cooldown.toSeconds());
    }

    /** Dọn khi gửi lỗi. expectedHash != null → chỉ xóa nếu mã trong Redis vẫn là mã của lần gửi này. */
    private void release(String key, String expectedHash) {
        try {
            if (expectedHash != null) {
                String current = redis.opsForValue().get(key + ":code");
                if (current != null && !current.equals(expectedHash)) return; // đã có mã mới hơn, đừng xóa
            }
            redis.delete(List.of(key + ":code", key + ":payload", key + ":cooldown"));
        } catch (RuntimeException ex) {
            log.error("Không dọn được OTP key {}", key, ex);
        }
    }

    /** Kiểm tra OTP. Đúng → xóa mã (dùng 1 lần), trả payload. Sai → ném AuthException. */
    public String verify(Purpose purpose, String email, String otp) {
        String key = key(purpose, email);
        String expected = redis.opsForValue().get(key + ":code");
        if (expected == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "Code expired. Please request a new one.");
        }
        Long tries = redis.opsForValue().increment(key + ":tries"); // atomic
        if (tries != null && tries == 1) redis.expire(key + ":tries", ttl);
        if (tries != null && tries > maxAttempts) {
            clear(key);
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "OTP_TOO_MANY_ATTEMPTS",
                    "Too many wrong attempts. Please request a new code.");
        }
        boolean match = MessageDigest.isEqual( // so sánh hằng thời gian
                expected.getBytes(StandardCharsets.UTF_8), hash(email, otp).getBytes(StandardCharsets.UTF_8));
        if (!match) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "Code is incorrect.");
        }
        String payload = redis.opsForValue().get(key + ":payload");
        clear(key);
        return payload;
    }

    private void clear(String key) { // giữ lại :cooldown để vẫn chặn spam gửi lại
        redis.delete(java.util.List.of(key + ":code", key + ":tries", key + ":payload"));
    }

    private String key(Purpose purpose, String email) {
        return "otp:" + purpose + ":" + email;
    }

    private String hash(String email, String otp) { // salt bằng email
        return HexFormat.of().formatHex(tokenHasher.hash(email + ":" + otp));
    }
}