package com.cinema.notification.client;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class MovieDirectoryClient {

    public record MovieResponse(String title) {
    }

    private final RestClient restClient;

    public MovieDirectoryClient(@Value("${notification.movie.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Optional<String> findTitle(UUID movieId) {
        try {
            MovieResponse response = restClient.get()
                    .uri("/api/v1/movies/{movieId}", movieId)
                    .retrieve()
                    .body(MovieResponse.class);
            return Optional.ofNullable(response).map(MovieResponse::title).filter(StringUtils::hasText);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }
}
