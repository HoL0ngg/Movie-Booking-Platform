package com.cinema.ai.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** Adapter for movie-service public catalog reads. Throws {@link org.springframework.web.client.RestClientException}. */
public class MovieCatalogClient {

    private static final ParameterizedTypeReference<List<CatalogMovie>> MOVIES = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public MovieCatalogClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** All published movies (now showing and coming soon) with genre names. */
    public List<CatalogMovie> listPublishedMovies() {
        List<CatalogMovie> movies = restClient.get().uri("/api/v1/movies").retrieve().body(MOVIES);
        return movies == null ? List.of() : movies;
    }
}
