package com.cinema.movie.controller;

import com.cinema.movie.dto.MovieResponse;
import com.cinema.movie.service.MovieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<MovieResponse> getMovies(@RequestParam(required = false) String status) {
        return movieService.getMovies(status);
    }

    @GetMapping("/{movieId}")
    public MovieResponse getMovie(@PathVariable UUID movieId) {
        return movieService.getMovie(movieId);
    }
}
