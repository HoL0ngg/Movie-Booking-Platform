package com.cinema.movie.controller; // 1.1

import java.util.List; // 1.2
import java.util.UUID; // 1.3

import org.springframework.web.bind.annotation.GetMapping; // 1.4
import org.springframework.web.bind.annotation.PathVariable; // 1.5
import org.springframework.web.bind.annotation.RequestMapping; // 1.6
import org.springframework.web.bind.annotation.RequestParam; // 1.7
import org.springframework.web.bind.annotation.RestController; // 1.8

import com.cinema.movie.dto.MovieResponse; // 1.9
import com.cinema.movie.service.MovieService; // 1.10

@RestController // 1.11 Controller trả JSON
@RequestMapping("/api/v1/movies") // 1.12 Prefix chung
public class MovieController { // 1.13

    private final MovieService movieService; // 1.14 Logic ở service, controller mỏng

    public MovieController(MovieService movieService) { // 1.15 Constructor injection
        this.movieService = movieService; // 1.16
    }

    @GetMapping // 1.17 GET /api/v1/movies
    public List<MovieResponse> getMovies( // 1.18
            @RequestParam(required = false) String status, // 1.19 NOW_SHOWING | COMING_SOON (đã có)
            @RequestParam(required = false) String query, // 1.20 MỚI: tìm theo tiêu đề
            @RequestParam(required = false) String genre) { // 1.21 MỚI: lọc theo tên thể loại
        return movieService.getMovies(status, query, genre); // 1.22
    }

    @GetMapping("/{movieId}") // 1.23 GET /api/v1/movies/{movieId}
    public MovieResponse getMovie(@PathVariable UUID movieId) { // 1.24 Sai định dạng UUID → 400 (handler có sẵn)
        return movieService.getMovie(movieId); // 1.25
    }
}