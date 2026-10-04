package com.cinema.auth.config; // 2.1

import java.nio.charset.StandardCharsets; // 2.2 Nguồn khóa cố định cho encoder

import javax.crypto.SecretKey; // 2.3
import javax.crypto.spec.SecretKeySpec; // 2.4 Kiểu khóa đối xứng

import org.springframework.boot.context.properties.EnableConfigurationProperties; // 2.5 Tạo SecretKey từ byte[]
import org.springframework.context.annotation.Bean; // 2.6
import org.springframework.context.annotation.Configuration; // 2.7
import org.springframework.security.oauth2.jose.jws.MacAlgorithm; // 2.8
import org.springframework.security.oauth2.jwt.JwtDecoder; // 2.9 Enum thuật toán HMAC
import org.springframework.security.oauth2.jwt.JwtEncoder; // 2.10 Verify token
import org.springframework.security.oauth2.jwt.JwtValidators; // 2.11 Ký token
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder; // 2.12 Bộ validate (exp, nbf, iss)
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder; // 2.13

import com.nimbusds.jose.jwk.source.ImmutableSecret; // 2.14

@Configuration(proxyBeanMethods = false) // 2.15 Class chứa @Bean, không cần proxy
@EnableConfigurationProperties(JwtProperties.class) // 2.16 Tạo bean JwtProperties từ YAML
public class JwtConfiguration { // 2.17

    @Bean // 2.18 Khóa ký thành bean
    SecretKey jwtSecretKey(JwtProperties properties) { // 2.19
        byte[] bytes = properties.secret().getBytes(StandardCharsets.UTF_8); // 2.20
        if (bytes.length < 32) { // 2.21 HS256 cần >= 256 bit
            throw new IllegalStateException("auth.jwt.secret phải >= 32 byte (đặt AUTH_JWT_SECRET)"); // 2.22 Fail-fast khi khởi động
        }
        return new SecretKeySpec(bytes, "HmacSHA256"); // 2.23
    }

    @Bean // 2.24
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) { // 2.25
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey)); // 2.26 Encoder ký bằng khóa này
    }

    @Bean // 2.27
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) { // 2.28
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey) // 2.29
                .macAlgorithm(MacAlgorithm.HS256) // 2.30 Chỉ nhận HS256
                .build(); // 2.31
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer())); // 2.32 Kiểm tra hạn + issuer
        return decoder; // 2.33
    }
}