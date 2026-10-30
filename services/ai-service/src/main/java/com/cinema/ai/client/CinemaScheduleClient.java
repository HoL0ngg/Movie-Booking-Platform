package com.cinema.ai.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** Adapter for cinema-service public cinema and showtime reads. Throws {@link org.springframework.web.client.RestClientException}. */
public class CinemaScheduleClient {

    private static final ParameterizedTypeReference<List<CatalogCinema>> CINEMAS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<CatalogShowtime>> SHOWTIMES = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public CinemaScheduleClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<CatalogCinema> listCinemas() {
        List<CatalogCinema> cinemas = restClient.get().uri("/api/v1/cinemas").retrieve().body(CINEMAS);
        return cinemas == null ? List.of() : cinemas;
    }

    /** Published showtimes of every cinema on {@code date}, evaluated in each cinema's timezone by the owner. */
    public List<CatalogShowtime> listPublishedShowtimes(LocalDate date) {
        List<CatalogShowtime> showtimes = restClient.get()
                .uri(uri -> uri.path("/api/v1/showtimes").queryParam("date", date.toString()).build())
                .retrieve()
                .body(SHOWTIMES);
        return showtimes == null ? List.of() : showtimes;
    }
}
