package com.cinema.movie.controller;

import com.cinema.movie.dto.AdminMovieRequest;
import com.cinema.movie.dto.AdminMovieResponse;
import com.cinema.movie.dto.GenreResponse;
import com.cinema.movie.service.AdminMovieService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMovieController {
    private final AdminMovieService service;

    public AdminMovieController(AdminMovieService service) { this.service = service; }

    @GetMapping("/movies")
    public List<AdminMovieResponse> list() { return service.list(); }

    @GetMapping("/movies/{movieId}")
    public AdminMovieResponse get(@PathVariable UUID movieId) { return service.get(movieId); }

    @GetMapping("/genres")
    public List<GenreResponse> genres() { return service.genres(); }

    @PostMapping("/movies")
    public ResponseEntity<AdminMovieResponse> create(@Valid @RequestBody AdminMovieRequest input) {
        AdminMovieResponse movie = service.create(input);
        return ResponseEntity.created(URI.create("/api/v1/admin/movies/" + movie.id())).body(movie);
    }

    @PutMapping("/movies/{movieId}")
    public AdminMovieResponse update(@PathVariable UUID movieId, @Valid @RequestBody AdminMovieRequest input) {
        return service.update(movieId, input);
    }

    @DeleteMapping("/movies/{movieId}")
    public ResponseEntity<Void> archive(@PathVariable UUID movieId) {
        service.archive(movieId);
        return ResponseEntity.noContent().build();
    }
}
