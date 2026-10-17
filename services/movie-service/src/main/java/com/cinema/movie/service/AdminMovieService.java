package com.cinema.movie.service;

import com.cinema.movie.dto.AdminMovieRequest;
import com.cinema.movie.dto.AdminMovieResponse;
import com.cinema.movie.dto.GenreResponse;
import com.cinema.movie.entity.MovieEntity;
import com.cinema.movie.entity.MovieGenreEntity;
import com.cinema.movie.repository.GenreRepository;
import com.cinema.movie.repository.MovieGenreRepository;
import com.cinema.movie.repository.MovieRepository;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminMovieService {
    private final MovieRepository movies;
    private final GenreRepository genres;
    private final MovieGenreRepository memberships;

    public AdminMovieService(MovieRepository movies, GenreRepository genres, MovieGenreRepository memberships) {
        this.movies = movies;
        this.genres = genres;
        this.memberships = memberships;
    }

    public List<AdminMovieResponse> list() {
        List<MovieEntity> rows = movies.findAll();
        Map<UUID, List<UUID>> genreIds = rows.isEmpty() ? Map.of() : memberships
                .findGenreNamesByMovieIds(rows.stream().map(MovieEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(MovieGenreRepository.MovieGenreName::getMovieId,
                        Collectors.mapping(MovieGenreRepository.MovieGenreName::getGenreId, Collectors.toList())));
        return rows.stream().sorted(Comparator.comparing(MovieEntity::getTitle).thenComparing(MovieEntity::getId))
                .map(movie -> response(movie, genreIds.getOrDefault(movie.getId(), List.of()))).toList();
    }

    public AdminMovieResponse get(UUID id) {
        MovieEntity movie = movies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return response(movie, memberships.findGenreNamesByMovieIds(List.of(id)).stream()
                .map(MovieGenreRepository.MovieGenreName::getGenreId).toList());
    }

    public List<GenreResponse> genres() {
        return genres.findAll().stream().map(genre -> new GenreResponse(genre.getId(), genre.getName()))
                .sorted(Comparator.comparing(GenreResponse::name)).toList();
    }

    @Transactional
    public AdminMovieResponse create(AdminMovieRequest input) {
        validateGenres(input.genreIds());
        MovieEntity movie = MovieEntity.create();
        apply(movie, input);
        movies.saveAndFlush(movie);
        saveGenres(movie.getId(), input.genreIds());
        return response(movie, input.genreIds());
    }

    @Transactional
    public AdminMovieResponse update(UUID id, AdminMovieRequest input) {
        MovieEntity movie = locked(id);
        validateGenres(input.genreIds());
        apply(movie, input);
        memberships.deleteByMovieId(id);
        saveGenres(id, input.genreIds());
        return response(movie, input.genreIds());
    }

    @Transactional
    public void archive(UUID id) {
        locked(id).archive();
    }

    private MovieEntity locked(UUID id) {
        return movies.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validateGenres(List<UUID> ids) {
        if (new HashSet<>(ids).size() != ids.size() || genres.findAllById(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void apply(MovieEntity movie, AdminMovieRequest input) {
        movie.update(input.title(), input.synopsis(), input.posterUrl(), input.durationMinutes(),
                input.releaseDate() != null ? input.releaseDate() : LocalDate.now(ZoneOffset.UTC), input.status());
    }

    private void saveGenres(UUID id, List<UUID> genreIds) {
        memberships.saveAll(genreIds.stream().map(genreId -> new MovieGenreEntity(id, genreId)).toList());
    }

    private AdminMovieResponse response(MovieEntity movie, List<UUID> genreIds) {
        return new AdminMovieResponse(movie.getId(), movie.getTitle(), movie.getSynopsis(), movie.getPosterUrl(),
                movie.getDurationMinutes(), movie.getReleaseDate(), movie.getStatus(), genreIds);
    }
}
