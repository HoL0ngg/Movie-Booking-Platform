package com.cinema.cinema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

import com.cinema.cinema.dto.CreateShowtimeRequest;
import com.cinema.cinema.entity.AuditoriumEntity;
import com.cinema.cinema.entity.SeatEntity;
import com.cinema.cinema.entity.ShowtimeEntity;
import com.cinema.cinema.exception.CinemaException;
import com.cinema.cinema.repository.AuditoriumRepository;
import com.cinema.cinema.repository.CinemaRepository;
import com.cinema.cinema.repository.SeatRepository;
import com.cinema.cinema.repository.ShowtimeRepository;
import com.cinema.cinema.service.CinemaAuthorizationService;
import com.cinema.cinema.service.OutboxService;
import com.cinema.cinema.service.ShowtimeService;

class ShowtimeRulesTests {
    private final ShowtimeRepository repository = mock(ShowtimeRepository.class);
    private final AuditoriumRepository auditoriums = mock(AuditoriumRepository.class);
    private final SeatRepository seats = mock(SeatRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final ShowtimeService service = new ShowtimeService(repository, auditoriums, mock(CinemaRepository.class),
            seats, outbox, mock(CinemaAuthorizationService.class));
    private final UUID roomId = UUID.randomUUID();
    private final UUID showtimeId = UUID.randomUUID();
    private final Instant start = Instant.parse("2035-01-01T10:00:00Z");
    private final Jwt admin = Jwt.withTokenValue("test").header("alg", "none")
            .subject(UUID.randomUUID().toString()).claim("roles", List.of("ADMIN")).build();

    @Test
    void listingAndEditingRequireAdminBeforeReadingData() {
        Jwt customer = Jwt.withTokenValue("test").header("alg", "none").subject("customer")
                .claim("roles", List.of("CUSTOMER")).build();
        assertThatThrownBy(() -> service.listForAdmin(customer, null)).isInstanceOf(CinemaException.class);
        assertThatThrownBy(() -> service.updateDraft(customer, showtimeId, request(roomId, start.plusSeconds(7200))))
                .isInstanceOf(CinemaException.class);
        verifyNoInteractions(repository, auditoriums, outbox);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUBLISHED", "CANCELLED"})
    void publishedAndCancelledSchedulesRemainImmutable(String state) {
        ShowtimeEntity showtime = load(state);
        UUID originalMovie = showtime.getMovieId();
        assertThatThrownBy(() -> service.updateDraft(admin, showtimeId, request(roomId, start.plusSeconds(7200))))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Only draft");
        assertThat(showtime.getMovieId()).isEqualTo(originalMovie);
        assertThat(showtime.getStartsAt()).isEqualTo(start);
        verifyNoInteractions(outbox);
    }

    @Test
    void editingDraftRejectsRoomChangeAndInvalidTimesThenAcceptsValidInput() {
        ShowtimeEntity showtime = load("DRAFT");
        assertThatThrownBy(() -> service.updateDraft(admin, showtimeId, request(UUID.randomUUID(), start.plusSeconds(7200))))
                .isInstanceOf(CinemaException.class).hasMessageContaining("auditorium");
        assertThatThrownBy(() -> service.updateDraft(admin, showtimeId, request(roomId, start)))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Invalid showtime times");
        CreateShowtimeRequest valid = request(roomId, start.plusSeconds(10800));
        assertThat(service.updateDraft(admin, showtimeId, valid).movieId()).isEqualTo(valid.movieId().toString());
        assertThat(showtime.getEndsAt()).isEqualTo(valid.endsAt());
        verifyNoInteractions(outbox);
    }

    @Test
    void legacyCoupleWithoutPartnerCannotPublishOrCreateOutbox() {
        ShowtimeEntity draft = load("DRAFT");
        when(seats.findActiveByAuditoriumId(roomId)).thenReturn(List.of(
                new SeatEntity(UUID.randomUUID(), roomId, "A", 1, "COUPLE", true, null, start)));
        assertThatThrownBy(() -> service.publish(admin, showtimeId))
                .isInstanceOf(CinemaException.class).hasMessageContaining("Couple seats");
        assertThat(draft.getStatus()).isEqualTo("DRAFT");
        verifyNoInteractions(outbox);
    }

    private ShowtimeEntity load(String state) {
        ShowtimeEntity showtime = new ShowtimeEntity(showtimeId, roomId, UUID.randomUUID(), start,
                start.plusSeconds(7200), start.minusSeconds(900), 100000L, "VND", state, start);
        when(repository.findAuditoriumIdById(showtimeId)).thenReturn(Optional.of(roomId));
        when(auditoriums.findForUpdateById(roomId))
                .thenReturn(Optional.of(new AuditoriumEntity(roomId, UUID.randomUUID(), "Screen 1", start)));
        when(repository.findForUpdateById(showtimeId)).thenReturn(Optional.of(showtime));
        return showtime;
    }

    private CreateShowtimeRequest request(UUID room, Instant end) {
        return new CreateShowtimeRequest(room, UUID.randomUUID(), start, end, start.minusSeconds(900), 150000L, "VND");
    }
}
