package com.cinema.cinema.config; // 1.1

import java.nio.charset.StandardCharsets; // 1.2

import javax.crypto.spec.SecretKeySpec; // 1.3 Tạo khóa HMAC từ byte[]

import org.springframework.beans.factory.annotation.Value; // 1.4 Đọc giá trị từ application.yml
import org.springframework.context.annotation.Bean; // 1.5
import org.springframework.context.annotation.Configuration; // 1.6
import org.springframework.security.oauth2.jose.jws.MacAlgorithm; // 1.7
import org.springframework.security.oauth2.jwt.JwtDecoder; // 1.8
import org.springframework.security.oauth2.jwt.JwtValidators; // 1.9
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder; // 1.10

@Configuration(proxyBeanMethods = false) // 1.11 Class khai báo bean
public class JwtConfiguration { // 1.12

    @Bean // 1.13
    JwtDecoder jwtDecoder(@Value("${auth.jwt.secret}") String secret, @Value("${auth.jwt.issuer}") String issuer) { // 1.14 Inject 2 giá trị cấu hình
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8); // 1.15
        if (bytes.length < 32) { // 1.16 HS256 cần >= 256 bit
            throw new IllegalStateException("auth.jwt.secret phải >= 32 byte (đặt AUTH_JWT_SECRET)"); // 1.17 Fail-fast
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256")) // 1.18 Verify bằng khóa đối xứng
                .macAlgorithm(MacAlgorithm.HS256) // 1.19 Chỉ nhận HS256
                .build(); // 1.20
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer)); // 1.21 Kiểm tra hạn + issuer
        return decoder; // 1.22
    }
}