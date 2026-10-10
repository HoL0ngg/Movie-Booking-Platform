package com.cinema.notification.client; // 6.1

import java.time.Duration; // 6.2
import java.util.Optional; // 6.3
import java.util.UUID; // 6.4

import org.springframework.beans.factory.annotation.Value; // 6.5
import org.springframework.http.client.SimpleClientHttpRequestFactory; // 6.6 Factory có đặt timeout
import org.springframework.stereotype.Component; // 6.7
import org.springframework.util.StringUtils; // 6.8
import org.springframework.web.client.HttpClientErrorException; // 6.9
import org.springframework.web.client.RestClient; // 6.10 HTTP client đồng bộ

@Component // 6.11
public class UserDirectoryClient { // 6.12 Adapter gọi API nội bộ của auth-service

    public record UserEmailResponse(String email) { // 6.13 Khớp JSON auth trả về
    }

    private final RestClient restClient; // 6.14
    private final String internalToken; // 6.15

    public UserDirectoryClient(@Value("${notification.auth.base-url}") String baseUrl, // 6.16
                               @Value("${notification.auth.internal-token:}") String internalToken) { // 6.17
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory(); // 6.18
        factory.setConnectTimeout(Duration.ofSeconds(2)); // 6.19 Tối đa 2 giây để kết nối
        factory.setReadTimeout(Duration.ofSeconds(3)); // 6.20 Tối đa 3 giây chờ phản hồi
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build(); // 6.21
        this.internalToken = internalToken; // 6.22
    }

    public Optional<String> findEmail(UUID userId) { // 6.23
        try { // 6.24
            UserEmailResponse response = restClient.get() // 6.25
                    .uri("/internal/v1/users/{id}/email", userId) // 6.26 {id} được thay bằng userId
                    .header("X-Internal-Token", internalToken) // 6.27 Chứng minh là service nội bộ
                    .retrieve() // 6.28 Gửi request; mã 4xx/5xx sẽ ném exception
                    .body(UserEmailResponse.class); // 6.29 JSON → record
            return Optional.ofNullable(response).map(UserEmailResponse::email).filter(StringUtils::hasText); // 6.30 Bỏ email rỗng
        } catch (HttpClientErrorException.NotFound ex) { // 6.31 404: user không tồn tại → không thể gửi
            return Optional.empty(); // 6.32
        }
        // Lỗi khác (401, 5xx, timeout) không bắt: lan ra để Kafka retry rồi quarantine
    }
}