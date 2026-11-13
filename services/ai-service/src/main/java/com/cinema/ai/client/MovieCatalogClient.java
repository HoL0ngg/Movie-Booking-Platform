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

    /** Published movies with optional owner-supported filters and genre names. */
    public List<CatalogMovie> listPublishedMovies(String status, String query, String genre) {
        List<CatalogMovie> movies = restClient.get().uri(uri -> {
            uri.path("/api/v1/movies");
            if (status != null) uri.queryParam("status", "{status}");
            if (query != null) uri.queryParam("query", "{query}");
            if (genre != null) uri.queryParam("genre", "{genre}");
            java.util.Map<String, String> values = new java.util.HashMap<>();
            if (status != null) values.put("status", status);
            if (query != null) values.put("query", query);
            if (genre != null) values.put("genre", genre);
            return uri.build(values);
        }).retrieve().body(MOVIES);
        return movies == null ? List.of() : movies;
    }
}
