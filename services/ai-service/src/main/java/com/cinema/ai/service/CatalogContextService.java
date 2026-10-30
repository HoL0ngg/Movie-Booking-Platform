package com.cinema.ai.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.cinema.ai.client.CatalogCinema;
import com.cinema.ai.client.CatalogMovie;
import com.cinema.ai.client.CatalogShowtime;
import com.cinema.ai.client.CinemaScheduleClient;
import com.cinema.ai.client.MovieCatalogClient;
import com.cinema.ai.config.CatalogProperties;

/**
 * Builds the read-only catalog grounding block for the chat prompt from owner APIs.
 * Failures of a source degrade to an explicit "unavailable" note; they never fail the chat.
 * The block is cached briefly so one conversation turn does not amplify into repeated
 * downstream calls. Model output based on it is a suggestion, never proof of availability.
 */
@Service
public class CatalogContextService {

    private static final Logger log = LoggerFactory.getLogger(CatalogContextService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String UNAVAILABLE = "tạm thời không tra cứu được, hãy nói rõ với người dùng";
    private static final String NEWLINE = "\n";

    private record Snapshot(String text, Instant expiresAt) {
    }

    private final MovieCatalogClient movies;
    private final CinemaScheduleClient cinemas;
    private final CatalogProperties properties;
    private final ZoneId defaultZone;
    private final AtomicReference<Snapshot> cache = new AtomicReference<>();

    public CatalogContextService(MovieCatalogClient movies, CinemaScheduleClient cinemas, CatalogProperties properties) {
        this.movies = movies;
        this.cinemas = cinemas;
        this.properties = properties;
        this.defaultZone = ZoneId.of(properties.timezone());
    }

    public String currentContext() {
        Instant now = Instant.now();
        Snapshot snapshot = cache.get();
        if (snapshot != null && now.isBefore(snapshot.expiresAt())) {
            return snapshot.text();
        }
        snapshot = build(now);
        cache.set(snapshot);
        return snapshot.text();
    }

    private Snapshot build(Instant now) {
        boolean complete = true;
        StringBuilder text = new StringBuilder();
        ZonedDateTime localNow = now.atZone(defaultZone);
        text.append("DỮ LIỆU THẬT (lấy lúc ").append(DAY_TIME.format(localNow)).append(", múi giờ ")
                .append(defaultZone.getId()).append("):").append(NEWLINE);

        Map<UUID, CatalogMovie> movieById = new LinkedHashMap<>();
        try {
            List<CatalogMovie> list = movies.listPublishedMovies();
            list.forEach(movie -> movieById.put(movie.id(), movie));
            appendMovies(text, list);
        } catch (RestClientException ex) {
            complete = false;
            warn("movie-service", ex);
            text.append(NEWLINE).append("PHIM: ").append(UNAVAILABLE).append('.').append(NEWLINE);
        }

        Map<UUID, CatalogCinema> cinemaById = new LinkedHashMap<>();
        try {
            List<CatalogCinema> list = cinemas.listCinemas();
            list.forEach(cinema -> cinemaById.put(cinema.id(), cinema));
            appendCinemas(text, list);
        } catch (RestClientException ex) {
            complete = false;
            warn("cinema-service (cinemas)", ex);
            text.append(NEWLINE).append("RẠP: ").append(UNAVAILABLE).append('.').append(NEWLINE);
        }

        LocalDate today = localNow.toLocalDate();
        LocalDate lastDay = today.plusDays(Math.max(0, properties.scheduleDays() - 1));
        text.append(NEWLINE).append("LỊCH CHIẾU ĐÃ CÔNG BỐ từ ").append(DAY.format(today)).append(" đến ")
                .append(DAY.format(lastDay))
                .append(" (giờ địa phương của rạp; ngoài khoảng này bạn không có dữ liệu):").append(NEWLINE);
        if (movieById.isEmpty() || cinemaById.isEmpty()) {
            text.append("- không có lịch chiếu để hiển thị.").append(NEWLINE);
        } else {
            try {
                List<CatalogShowtime> schedule = new ArrayList<>();
                for (LocalDate day = today; !day.isAfter(lastDay); day = day.plusDays(1)) {
                    schedule.addAll(cinemas.listPublishedShowtimes(day));
                }
                appendShowtimes(text, schedule, movieById, cinemaById);
            } catch (RestClientException ex) {
                complete = false;
                warn("cinema-service (showtimes)", ex);
                text.append("- ").append(UNAVAILABLE).append('.').append(NEWLINE);
            }
        }
        Instant expiresAt = now.plus(complete ? properties.cacheTtl() : properties.failureCacheTtl());
        return new Snapshot(text.toString(), expiresAt);
    }

    private void appendMovies(StringBuilder text, List<CatalogMovie> list) {
        text.append(NEWLINE).append("PHIM ĐANG CHIẾU / SẮP CHIẾU (").append(list.size()).append(" phim):")
                .append(NEWLINE);
        if (list.isEmpty()) {
            text.append("- chưa có phim nào trong danh mục.").append(NEWLINE);
        }
        int shown = 0;
        for (CatalogMovie movie : list) {
            if (shown++ >= properties.maxMovies()) {
                text.append("- ... và ").append(list.size() - properties.maxMovies())
                        .append(" phim khác chưa liệt kê.").append(NEWLINE);
                break;
            }
            text.append("- ").append(movie.title())
                    .append(" | thể loại: ")
                    .append(movie.genres().isEmpty() ? "chưa phân loại" : String.join(", ", movie.genres()))
                    .append(movie.durationMinutes() == null ? "" : " | " + movie.durationMinutes() + " phút")
                    .append(" | ").append("COMING_SOON".equals(movie.status()) ? "SẮP CHIẾU" : "ĐANG CHIẾU")
                    .append(movie.releaseDate() == null ? "" : " | khởi chiếu " + movie.releaseDate())
                    .append(NEWLINE);
        }
    }

    private void appendCinemas(StringBuilder text, List<CatalogCinema> list) {
        text.append(NEWLINE).append("RẠP (").append(list.size()).append("):").append(NEWLINE);
        if (list.isEmpty()) {
            text.append("- chưa có rạp nào.").append(NEWLINE);
        }
        for (CatalogCinema cinema : list) {
            text.append("- ").append(cinema.name()).append(" | ").append(cinema.city())
                    .append(" | ").append(cinema.address()).append(NEWLINE);
        }
    }

    private void appendShowtimes(StringBuilder text, List<CatalogShowtime> schedule, Map<UUID, CatalogMovie> movieById,
            Map<UUID, CatalogCinema> cinemaById) {
        int shown = 0;
        int skipped = 0;
        for (CatalogShowtime showtime : schedule) {
            CatalogMovie movie = movieById.get(showtime.movieId());
            CatalogCinema cinema = cinemaById.get(showtime.cinemaId());
            if (movie == null || cinema == null) {
                skipped++; // Movie no longer public or unknown cinema: never surface it.
                continue;
            }
            if (shown >= properties.maxShowtimes()) {
                text.append("- ... còn ").append(schedule.size() - skipped - shown)
                        .append(" suất khác chưa liệt kê.").append(NEWLINE);
                return;
            }
            shown++;
            ZoneId zone = safeZone(cinema.timezone());
            text.append("- ").append(DAY_TIME.format(showtime.startsAt().atZone(zone)))
                    .append(" | ").append(movie.title())
                    .append(" | ").append(cinema.name()).append(" (").append(cinema.city()).append(')')
                    .append(showtime.price() == null ? ""
                            : " | giá niêm yết " + showtime.price().amountMinor() + " " + showtime.price().currency())
                    .append(NEWLINE);
        }
        if (shown == 0) {
            text.append("- không có suất chiếu đã công bố trong khoảng này.").append(NEWLINE);
        }
    }

    private ZoneId safeZone(String timezone) {
        try {
            return timezone == null ? defaultZone : ZoneId.of(timezone);
        } catch (RuntimeException ex) {
            return defaultZone;
        }
    }

    private void warn(String source, RestClientException ex) {
        log.warn("Catalog source unavailable for AI grounding: source={} exceptionType={}", source,
                ex.getClass().getSimpleName());
    }
}
