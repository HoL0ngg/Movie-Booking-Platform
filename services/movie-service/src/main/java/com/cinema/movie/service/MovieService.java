package com.cinema.movie.service;

import com.cinema.movie.entity.MovieEntity;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.dto.MovieResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository){
        this.movieRepository = movieRepository;
    }

    public List<MovieResponse> getMovies(String status) {
        if (status != null && !status.isBlank()
                && !List.of("NOW_SHOWING", "COMING_SOON").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return movieRepository.findByStatus("PUBLISHED").stream()
                .map(movie -> toResponse(movie, today))
                .filter(movie -> status == null || status.isBlank() || movie.status().equals(status))
                .toList();
    }

    public MovieResponse getMovie(UUID id) {
        MovieEntity movie = movieRepository.findByIdAndStatus(id, "PUBLISHED")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(movie, LocalDate.now(ZoneOffset.UTC));
    }

    private MovieResponse toResponse(MovieEntity movie, LocalDate today) {
        String status = movie.getReleaseDate() != null && movie.getReleaseDate().isAfter(today)
                ? "COMING_SOON" : "NOW_SHOWING";
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getSynopsis(),
                movie.getDurationMinutes(), movie.getReleaseDate(), status);
    }
}
