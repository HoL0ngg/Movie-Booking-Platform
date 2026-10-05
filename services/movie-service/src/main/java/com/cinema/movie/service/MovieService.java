package com.cinema.movie.service; // 2.1

import com.cinema.movie.dto.MovieResponse; // 2.2
import com.cinema.movie.entity.MovieEntity; // 2.3
import com.cinema.movie.repository.MovieGenreRepository; // 2.4
import com.cinema.movie.repository.MovieRepository; // 2.5
import java.time.LocalDate; // 2.6
import java.time.ZoneOffset; // 2.7
import java.util.Comparator; // 2.8
import java.util.List; // 2.9
import java.util.Locale; // 2.10
import java.util.Map; // 2.11
import java.util.UUID; // 2.12
import java.util.stream.Collectors; // 2.13
import org.springframework.http.HttpStatus; // 2.14
import org.springframework.stereotype.Service; // 2.15
import org.springframework.transaction.annotation.Transactional; // 2.16
import org.springframework.util.StringUtils; // 2.17
import org.springframework.web.server.ResponseStatusException; // 2.18 Handler có sẵn đổi thành ApiError chuẩn

@Service // 2.19 Bean nghiệp vụ
@Transactional(readOnly = true) // 2.20 Mọi method chỉ đọc
public class MovieService { // 2.21

    private static final String PUBLISHED = "PUBLISHED"; // 2.22 Chỉ phim PUBLISHED hiển thị công khai

    private final MovieRepository movieRepository; // 2.23
    private final MovieGenreRepository movieGenreRepository; // 2.24

    public MovieService(MovieRepository movieRepository, MovieGenreRepository movieGenreRepository) { // 2.25
        this.movieRepository = movieRepository; // 2.26
        this.movieGenreRepository = movieGenreRepository; // 2.27
    }

    public List<MovieResponse> getMovies(String status, String query, String genre) { // 2.28
        if (status != null && !status.isBlank() // 2.29 Giữ nguyên kiểm tra status của bản cũ
                && !List.of("NOW_SHOWING", "COMING_SOON").contains(status)) { // 2.30
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST); // 2.31 → 400 VALIDATION_ERROR
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC); // 2.32 Ngày hiện tại (UTC) để suy ra trạng thái hiển thị
        String keyword = normalize(query); // 2.33 Từ khóa chữ thường, null nếu không nhập
        String genreName = normalize(genre); // 2.34
        List<MovieEntity> published = movieRepository.findByStatus(PUBLISHED); // 2.35 Lấy phim đã công bố
        Map<UUID, List<String>> genresByMovie = genreNames(published.stream().map(MovieEntity::getId).toList()); // 2.36 Thể loại của tất cả phim bằng 1 truy vấn (tránh N+1)
        return published.stream() // 2.37
                .filter(movie -> keyword == null || movie.getTitle().toLowerCase(Locale.ROOT).contains(keyword)) // 2.38 Tiêu đề chứa từ khóa
                .map(movie -> toResponse(movie, genresByMovie.getOrDefault(movie.getId(), List.of()), today)) // 2.39 Entity → DTO (phim chưa gán thể loại = danh sách rỗng)
                .filter(movie -> !StringUtils.hasText(status) || movie.status().equals(status)) // 2.40 Lọc theo trạng thái hiển thị
                .filter(movie -> genreName == null // 2.41 Lọc theo thể loại (không phân biệt hoa thường)
                        || movie.genres().stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).equals(genreName))) // 2.42
                .sorted(Comparator.comparing(MovieResponse::releaseDate, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())) // 2.43 Mới chiếu trước, chưa có ngày xuống cuối
                        .thenComparing(MovieResponse::title)) // 2.44 Cùng ngày thì theo tên: thứ tự ổn định
                .toList(); // 2.45
    }

    public MovieResponse getMovie(UUID id) { // 2.46
        MovieEntity movie = movieRepository.findByIdAndStatus(id, PUBLISHED) // 2.47
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); // 2.48 → 404 MOVIE_NOT_FOUND (phim nháp coi như không tồn tại)
        List<String> genres = genreNames(List.of(movie.getId())).getOrDefault(movie.getId(), List.of()); // 2.49
        return toResponse(movie, genres, LocalDate.now(ZoneOffset.UTC)); // 2.50
    }

    private Map<UUID, List<String>> genreNames(List<UUID> movieIds) { // 2.51 movieId → danh sách tên thể loại
        if (movieIds.isEmpty()) { // 2.52 Tránh IN () rỗng
            return Map.of(); // 2.53
        }
        return movieGenreRepository.findGenreNamesByMovieIds(movieIds).stream() // 2.54
                .collect(Collectors.groupingBy( // 2.55 Gom theo movieId
                        MovieGenreRepository.MovieGenreName::getMovieId, // 2.56 Khóa nhóm
                        Collectors.mapping(MovieGenreRepository.MovieGenreName::getName, Collectors.toList()))); // 2.57 Giá trị = tên thể loại
    }

    private MovieResponse toResponse(MovieEntity movie, List<String> genres, LocalDate today) { // 2.58
        String status = movie.getReleaseDate() != null && movie.getReleaseDate().isAfter(today) // 2.59 Ngày chiếu ở tương lai?
                ? "COMING_SOON" : "NOW_SHOWING"; // 2.60 Quy tắc giữ nguyên bản cũ
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getSynopsis(), movie.getPosterUrl(), // 2.61
                movie.getDurationMinutes(), movie.getReleaseDate(), status, genres); // 2.62
    }

    private String normalize(String text) { // 2.63
        return StringUtils.hasText(text) ? text.trim().toLowerCase(Locale.ROOT) : null; // 2.64 Rỗng/khoảng trắng coi như không lọc
    }
}