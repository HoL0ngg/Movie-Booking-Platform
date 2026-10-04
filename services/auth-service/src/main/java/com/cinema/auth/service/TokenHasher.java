package com.cinema.auth.service; // 9.1

import java.nio.charset.StandardCharsets; // 9.2
import java.security.MessageDigest; // 9.3 Băm SHA-256
import java.security.NoSuchAlgorithmException; // 9.4
import java.security.SecureRandom; // 9.5 Sinh số ngẫu nhiên mật mã
import java.util.Base64; // 9.6

import org.springframework.stereotype.Component; // 9.7

@Component // 9.8 Bean tiện ích
public class TokenHasher { // 9.9

    private final SecureRandom random = new SecureRandom(); // 9.10 Dùng chung, thread-safe

    public String newToken() { // 9.11 Tạo refresh token mới
        byte[] bytes = new byte[32]; // 9.12 256 bit
        random.nextBytes(bytes); // 9.13
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 9.14 Chuỗi URL-safe
    }

    public byte[] hash(String token) { // 9.15 Token → hash lưu DB
        try { // 9.16
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)); // 9.17
        } catch (NoSuchAlgorithmException ex) { // 9.18 JVM nào cũng có SHA-256
            throw new IllegalStateException(ex); // 9.19
        }
    }
}