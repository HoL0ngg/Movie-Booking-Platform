package com.cinema.auth.service; // 10.1

import java.time.Instant; // 10.2
import java.util.List; // 10.3
import java.util.UUID; // 10.4

import org.springframework.security.oauth2.jose.jws.MacAlgorithm; // 10.5
import org.springframework.security.oauth2.jwt.JwsHeader; // 10.6
import org.springframework.security.oauth2.jwt.JwtClaimsSet; // 10.7 Header JWT
import org.springframework.security.oauth2.jwt.JwtEncoder; // 10.8 Payload JWT
import org.springframework.security.oauth2.jwt.JwtEncoderParameters; // 10.9
import org.springframework.stereotype.Service; // 10.10 Gói header + claims

import com.cinema.auth.config.JwtProperties; // 10.11

@Service // 10.12
public class JwtService { // 10.13

    private final JwtEncoder jwtEncoder; // 10.14 Bean từ JwtConfiguration
    private final JwtProperties properties; // 10.15

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) { // 10.16
        this.jwtEncoder = jwtEncoder; // 10.17
        this.properties = properties; // 10.18
    }

    public String createAccessToken(UUID userId, String email, List<String> roles, Instant now) { // 10.19
        JwtClaimsSet claims = JwtClaimsSet.builder() // 10.20
                .issuer(properties.issuer()) // 10.21 iss
                .subject(userId.toString()) // 10.22 sub = userId (service khác dùng làm định danh)
                .issuedAt(now) // 10.23 iat
                .expiresAt(now.plus(properties.accessTtl())) // 10.24 exp
                .claim("email", email) // 10.25
                .claim("roles", roles) // 10.26 Khớp claim name ở SecurityConfiguration (3.49)
                .build(); // 10.27
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build(); // 10.28 Header alg=HS256
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(); // 10.29 Ký và trả chuỗi JWT
    }
}