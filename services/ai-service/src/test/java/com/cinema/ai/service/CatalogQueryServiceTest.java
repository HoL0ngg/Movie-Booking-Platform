package com.cinema.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.List;
import java.util.UUID;

import com.cinema.ai.client.*;
import com.cinema.ai.config.CatalogProperties;
import com.cinema.ai.dto.CatalogQuery;
import com.cinema.ai.dto.CatalogQuery.Intent;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class CatalogQueryServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private final MovieCatalogClient movies = mock(MovieCatalogClient.class);
    private final CinemaScheduleClient cinemas = mock(CinemaScheduleClient.class);
    private final CatalogProperties properties = new CatalogProperties("http://movies", "http://cinemas",
            "Asia/Ho_Chi_Minh", Duration.ofSeconds(1), Duration.ofSeconds(2), 40, 80);
    private final CatalogQueryService service = new CatalogQueryService(movies, cinemas, properties,
            Clock.fixed(Instant.parse("2026-11-13T05:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
    private final UUID movieId = UUID.randomUUID();
    private final UUID cinemaId = UUID.randomUUID();

    @Test
    void filtersRealShowtimesByMovieCityLocalTimeAndPublishedFutureState() {
        when(movies.listPublishedMovies(null, null, "Kinh dị")).thenReturn(List.of(movie()));
        when(cinemas.listCinemas(null)).thenReturn(List.of(cinema()));
        when(cinemas.listPublishedShowtimes(TODAY, movieId)).thenReturn(List.of(
                showtime(movieId, cinemaId, "2026-11-13T13:00:00Z", "PUBLISHED"),
                showtime(movieId, cinemaId, "2026-11-13T11:00:00Z", "PUBLISHED"),
                showtime(movieId, cinemaId, "2026-11-13T14:00:00Z", "CANCELLED"),
                showtime(UUID.randomUUID(), cinemaId, "2026-11-13T15:00:00Z", "PUBLISHED"),
                showtime(movieId, UUID.randomUUID(), "2026-11-13T16:00:00Z", "PUBLISHED"),
                showtime(movieId, cinemaId, "2026-11-13T01:00:00Z", "PUBLISHED")));
        String answer = service.answer(new CatalogQuery(Intent.SHOWTIMES, null, "Kinh dị", "HCM", null,
                null, TODAY, TODAY, LocalTime.of(19, 0), null));
        assertThat(answer).contains("Phim thật", "Rạp thật", "13/11/2026 20:00", "100000 VND", "Chưa kiểm tra ghế trống")
                .doesNotContain("18:00", "21:00", "22:00", "23:00", "08:00");
        verify(cinemas).listPublishedShowtimes(TODAY, movieId);
    }

    @Test
    void rejectsInvalidRangesAndDroppedConstraintsWithoutAnyApiCalls() {
        List<CatalogQuery> invalid = List.of(
                query(Intent.SHOWTIMES, TODAY, TODAY.plusDays(8)),
                query(Intent.SHOWTIMES, TODAY.minusDays(1), TODAY),
                query(Intent.SHOWTIMES, TODAY.plusDays(91), null),
                query(Intent.SHOWTIMES, TODAY, TODAY.minusDays(1)),
                query(Intent.SHOWTIMES, null, TODAY.plusDays(1)),
                query(Intent.MOVIES, TODAY, null),
                new CatalogQuery(Intent.CINEMAS, "Phim", null, null, null, null, null, null, null, null),
                new CatalogQuery(Intent.SHOWTIMES, null, null, null, null, null, TODAY, TODAY,
                        LocalTime.of(22, 0), LocalTime.of(2, 0)),
                new CatalogQuery(Intent.MOVIES, "x".repeat(161), null, null, null, null, null, null, null, null));
        for (CatalogQuery q : invalid) assertThat(service.answer(q)).isEqualTo(CatalogQueryService.CLARIFICATION);
        assertThat(service.answer(null)).isEqualTo(CatalogQueryService.CLARIFICATION);
        verifyNoInteractions(movies, cinemas);
    }

    @Test
    void queriesExactlyEightDaysAndDoesNotQueryEachMovieOrCinemaSeparately() {
        when(movies.listPublishedMovies(null, null, null)).thenReturn(List.of(movie()));
        when(cinemas.listCinemas(null)).thenReturn(List.of(cinema()));
        when(cinemas.listPublishedShowtimes(any(), eq(movieId))).thenReturn(List.of());
        assertThat(service.answer(query(Intent.SHOWTIMES, TODAY, TODAY.plusDays(7))))
                .contains("Không có kết quả", "2026-11-20");
        verify(cinemas, times(8)).listPublishedShowtimes(any(), eq(movieId));
    }

    @Test
    void distinguishesApiFailureFromNoMatchesAndDoesNotReturnPartialSchedule() {
        when(movies.listPublishedMovies(null, null, null)).thenReturn(List.of(movie()));
        when(cinemas.listCinemas(null)).thenReturn(List.of(cinema()));
        when(cinemas.listPublishedShowtimes(TODAY, movieId))
                .thenReturn(List.of(showtime(movieId, cinemaId, "2026-11-13T13:00:00Z", "PUBLISHED")));
        when(cinemas.listPublishedShowtimes(TODAY.plusDays(1), movieId))
                .thenThrow(new ResourceAccessException("timeout"));
        assertThat(service.answer(query(Intent.SHOWTIMES, TODAY, TODAY.plusDays(1))))
                .contains("Tạm thời không tra cứu được").doesNotContain("Phim thật");
    }

    @Test
    void unsupportedRequestsCannotInvokeOwnerApis() {
        assertThat(service.answer(query(Intent.UNSUPPORTED, null, null))).contains("không thể kiểm tra ghế trống");
        verifyNoInteractions(movies, cinemas);
    }

    @Test
    void upcomingMoviesUseOwnerFilterAndDoNotLoadCinemasOrSchedule() {
        when(movies.listPublishedMovies("COMING_SOON", null, "Kinh dị")).thenReturn(List.of(movie()));
        assertThat(service.answer(new CatalogQuery(Intent.MOVIES, null, "Kinh dị", null, null,
                CatalogQuery.MovieStatus.COMING_SOON, null, null, null, null))).contains("Phim thật");
        verifyNoInteractions(cinemas);
    }

    @Test
    void cinemaSearchNormalizesCityAndDoesNotLoadMovies() {
        when(cinemas.listCinemas(null)).thenReturn(List.of(cinema()));
        assertThat(service.answer(new CatalogQuery(Intent.CINEMAS, null, null, "ho chi minh", "Rạp",
                null, null, null, null, null))).contains("Rạp thật");
        verifyNoInteractions(movies);
    }

    @Test
    void longResultsStayWithinConversationHistoryLimitAndDiscloseTruncation() {
        CatalogMovie longTitle = new CatalogMovie(movieId, "Phim ".repeat(30), 100, TODAY,
                "NOW_SHOWING", List.of("Kinh dị"));
        when(movies.listPublishedMovies(null, null, null))
                .thenReturn(java.util.Collections.nCopies(40, longTitle));
        String answer = service.answer(query(Intent.MOVIES, null, null));
        assertThat(answer.length()).isLessThanOrEqualTo(4000);
        assertThat(answer).contains("Đang hiển thị", "/40 kết quả");
    }

    private CatalogQuery query(Intent intent, LocalDate from, LocalDate to) {
        return new CatalogQuery(intent, null, null, null, null, null, from, to, null, null);
    }

    private CatalogMovie movie() {
        return new CatalogMovie(movieId, "Phim thật", 100, TODAY, "NOW_SHOWING", List.of("Kinh dị"));
    }

    private CatalogCinema cinema() {
        return new CatalogCinema(cinemaId, "Rạp thật", "Địa chỉ thật", "TP. Hồ Chí Minh", "Asia/Ho_Chi_Minh");
    }

    private CatalogShowtime showtime(UUID movie, UUID cinema, String start, String status) {
        return new CatalogShowtime(UUID.randomUUID(), movie, cinema, Instant.parse(start),
                Instant.parse(start).plusSeconds(6000), new CatalogShowtime.Money(100000, "VND"), status);
    }
}
