package com.cinema.ai.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.cinema.ai.client.CatalogCinema;
import com.cinema.ai.client.CatalogMovie;
import com.cinema.ai.client.CatalogShowtime;
import com.cinema.ai.client.CinemaScheduleClient;
import com.cinema.ai.client.MovieCatalogClient;
import com.cinema.ai.config.CatalogProperties;
import com.cinema.ai.dto.CatalogQuery;
import com.cinema.ai.dto.CatalogQuery.Intent;

/** Java owns validation, the read-only API allowlist, filtering and factual response rendering. */
@Service
public class CatalogQueryService {
    public static final String CLARIFICATION = "Bạn hãy nói rõ tên phim, thể loại, thành phố/rạp hoặc ngày giờ muốn xem nhé. "
            + "Mỗi lượt có thể tra tối đa 8 ngày liên tiếp trong 90 ngày tới; khoảng giờ cần nằm trong cùng một ngày.";
    private static final Logger log = LoggerFactory.getLogger(CatalogQueryService.class);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final MovieCatalogClient movies;
    private final CinemaScheduleClient cinemas;
    private final CatalogProperties properties;
    private final Clock clock;

    @Autowired
    public CatalogQueryService(MovieCatalogClient movies, CinemaScheduleClient cinemas, CatalogProperties properties) {
        this(movies, cinemas, properties, Clock.system(ZoneId.of(properties.timezone())));
    }

    CatalogQueryService(MovieCatalogClient movies, CinemaScheduleClient cinemas, CatalogProperties properties, Clock clock) {
        this.movies = movies;
        this.cinemas = cinemas;
        this.properties = properties;
        this.clock = clock;
    }

    public String answer(CatalogQuery query) {
        if (!valid(query)) return CLARIFICATION;
        try {
            return switch (query.intent()) {
                case GREETING -> "Chào bạn! Mình có thể gợi ý phim, tìm rạp và tra lịch chiếu đã công bố. Bạn muốn xem gì?";
                case CLARIFY -> CLARIFICATION;
                case UNSUPPORTED -> "Mình hỗ trợ tra cứu phim, rạp theo thành phố/tên và lịch chiếu đã công bố. "
                        + "Mình không thể kiểm tra ghế trống, giữ ghế, đặt vé hay thanh toán; hãy dùng trang đặt vé cho các thao tác đó.";
                case MOVIES -> movieAnswer(query);
                case CINEMAS -> cinemaAnswer(query);
                case SHOWTIMES -> showtimeAnswer(query);
            };
        } catch (RestClientException | java.time.DateTimeException unavailable) {
            log.warn("AI catalog query unavailable: intent={} exceptionType={}", query.intent(),
                    unavailable.getClass().getSimpleName());
            return "Tạm thời không tra cứu được dữ liệu phim/rạp/lịch chiếu. Bạn vui lòng thử lại sau; "
                    + "điều này không có nghĩa là không có kết quả.";
        }
    }

    private boolean valid(CatalogQuery q) {
        if (q == null || q.intent() == null) return false;
        for (String value : new String[] {q.movieQuery(), q.genre(), q.city(), q.cinemaQuery()}) {
            if (value != null && (value.isBlank() || value.length() > 160 || value.chars().anyMatch(Character::isISOControl)))
                return false;
        }
        // Do not silently drop constraints attached to the wrong intent.
        boolean scheduleFilters = q.dateFrom() != null || q.dateTo() != null || q.timeFrom() != null || q.timeTo() != null;
        if (q.intent() == Intent.MOVIES && (scheduleFilters || q.city() != null || q.cinemaQuery() != null)) return false;
        if (q.intent() == Intent.CINEMAS && (scheduleFilters || q.movieQuery() != null || q.genre() != null || q.status() != null)) return false;
        if (q.intent() != Intent.SHOWTIMES) return true;
        if (q.dateFrom() == null && q.dateTo() != null) return false;
        LocalDate today = LocalDate.now(clock);
        LocalDate from = q.dateFrom() == null ? today : q.dateFrom();
        LocalDate to = q.dateTo() == null ? from : q.dateTo();
        return !from.isBefore(today) && !to.isBefore(from) && !to.isAfter(today.plusDays(90))
                && !to.isAfter(from.plusDays(7))
                && (q.timeFrom() == null || q.timeTo() == null || !q.timeTo().isBefore(q.timeFrom()));
    }

    private String movieAnswer(CatalogQuery q) {
        List<CatalogMovie> found = movies.listPublishedMovies(q.status() == null ? null : q.status().name(),
                q.movieQuery(), q.genre());
        List<String> lines = found.stream().limit(properties.maxMovies()).map(m -> "- " + m.title()
                + " | " + String.join(", ", m.genres()) + " | "
                + ("COMING_SOON".equals(m.status()) ? "Sắp chiếu" : "Đang chiếu")
                + (m.releaseDate() == null ? "" : " | khởi chiếu " + m.releaseDate())).toList();
        return render("Phim phù hợp với yêu cầu:", lines, found.size());
    }

    private String cinemaAnswer(CatalogQuery q) {
        List<CatalogCinema> found = matchingCinemas(q);
        return render("Rạp phù hợp với yêu cầu:", found.stream().limit(20)
                .map(c -> "- " + c.name() + " | " + c.city() + " | " + c.address()).toList(), found.size());
    }

    private List<CatalogCinema> matchingCinemas(CatalogQuery q) {
        // Resolve city names against owner data; DB values may include 'TP.' or accents.
        return cinemas.listCinemas(null).stream()
                .filter(c -> q.city() == null || normalizedCity(c.city()).equals(normalizedCity(q.city())))
                .filter(c -> q.cinemaQuery() == null || c.name().toLowerCase(java.util.Locale.ROOT)
                        .contains(q.cinemaQuery().toLowerCase(java.util.Locale.ROOT))).toList();
    }

    private String normalizedCity(String city) {
        String normalized = java.text.Normalizer.normalize(city, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).replace('đ', 'd')
                .replaceAll("[^a-z0-9 ]", " ").trim().replaceAll("\\s+", " ")
                .replaceFirst("^(thanh pho|tp)\\s+", "");
        return switch (normalized) {
            case "hcm", "tphcm", "sai gon", "saigon", "ho chi minh city" -> "ho chi minh";
            case "hanoi" -> "ha noi";
            default -> normalized;
        };
    }

    private String showtimeAnswer(CatalogQuery q) {
        Map<UUID, CatalogMovie> movieById = movies.listPublishedMovies(
                q.status() == null ? null : q.status().name(), q.movieQuery(), q.genre()).stream()
                .collect(Collectors.toMap(CatalogMovie::id, Function.identity()));
        if (movieById.isEmpty()) return "Không tìm thấy phim phù hợp trong danh mục đã công bố.";
        Map<UUID, CatalogCinema> cinemaById = matchingCinemas(q).stream()
                .collect(Collectors.toMap(CatalogCinema::id, Function.identity()));
        if (cinemaById.isEmpty()) return "Không tìm thấy rạp phù hợp với yêu cầu.";
        LocalDate from = q.dateFrom() == null ? LocalDate.now(clock) : q.dateFrom();
        LocalDate to = q.dateTo() == null ? from : q.dateTo();
        UUID singleMovie = movieById.size() == 1 ? movieById.keySet().iterator().next() : null;
        List<CatalogShowtime> found = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            for (CatalogShowtime s : cinemas.listPublishedShowtimes(day, singleMovie)) {
                if (!"PUBLISHED".equals(s.status()) || !movieById.containsKey(s.movieId())
                        || !cinemaById.containsKey(s.cinemaId()) || !s.startsAt().isAfter(clock.instant())) continue;
                var local = s.startsAt().atZone(ZoneId.of(cinemaById.get(s.cinemaId()).timezone()));
                if (!local.toLocalDate().equals(day)) continue;
                if (q.timeFrom() != null && local.toLocalTime().isBefore(q.timeFrom())) continue;
                if (q.timeTo() != null && local.toLocalTime().isAfter(q.timeTo())) continue;
                found.add(s);
            }
        }
        List<String> lines = found.stream().sorted(Comparator.comparing(CatalogShowtime::startsAt))
                .limit(properties.maxShowtimes()).map(s -> {
                    CatalogCinema c = cinemaById.get(s.cinemaId());
                    return "- " + movieById.get(s.movieId()).title() + " | " + c.name() + " (" + c.city() + ") | "
                            + DISPLAY_TIME.format(s.startsAt().atZone(ZoneId.of(c.timezone()))) + " [" + c.timezone() + "]"
                            + (s.price() == null ? "" : " | giá niêm yết: " + s.price().amountMinor() + " " + s.price().currency());
                }).toList();
        return render("Lịch chiếu đã công bố từ " + from + " đến " + to + ":", lines, found.size())
                + "\nChưa kiểm tra ghế trống. Giá cuối cùng hiển thị khi thanh toán.";
    }

    private String render(String heading, List<String> lines, int total) {
        if (total == 0) return heading + "\nKhông có kết quả phù hợp trong dữ liệu đã công bố.";
        // Answers must also fit ChatMessage's 4000-character history limit on the next turn.
        StringBuilder text = new StringBuilder(heading);
        int shown = 0;
        for (String line : lines) {
            if (text.length() + line.length() + 1 > 3200) break;
            text.append('\n').append(line);
            shown++;
        }
        if (total > shown) text.append("\nĐang hiển thị ").append(shown).append('/').append(total)
                .append(" kết quả; hãy thu hẹp yêu cầu để xem thêm.");
        return text.toString();
    }
}
