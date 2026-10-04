package com.cinema.auth.config; // 1.1

import java.time.Duration; // 1.2 Kiểu thời lượng, đọc được "15m", "7d"

import org.springframework.boot.context.properties.ConfigurationProperties; // 1.3 Bind YAML → object

@ConfigurationProperties(prefix = "auth.jwt") // 1.4 Bind các key dưới auth.jwt
public record JwtProperties(String secret, String issuer, Duration accessTtl, Duration refreshTtl) { // 1.5 Record bất biến; access-ttl → accessTtl
}