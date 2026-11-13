package com.cinema.ai.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Untrusted model output. Never contains an executable URL, SQL, or remote ID. */
public record CatalogQuery(Intent intent, String movieQuery, String genre, String city, String cinemaQuery,
        MovieStatus status, LocalDate dateFrom, LocalDate dateTo, LocalTime timeFrom, LocalTime timeTo) {
    public enum Intent { MOVIES, CINEMAS, SHOWTIMES, CLARIFY, UNSUPPORTED, GREETING }
    public enum MovieStatus { NOW_SHOWING, COMING_SOON }
}
