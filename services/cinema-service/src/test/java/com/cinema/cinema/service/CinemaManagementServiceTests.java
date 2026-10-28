package com.cinema.cinema.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.cinema.cinema.dto.PhysicalSeatDto;
import com.cinema.cinema.dto.SaveSeatLayoutRequest;
import com.cinema.cinema.entity.AuditoriumEntity;
import com.cinema.cinema.exception.CinemaException;
import com.cinema.cinema.repository.AuditoriumRepository;
import com.cinema.cinema.repository.CinemaRepository;
import com.cinema.cinema.repository.SeatRepository;
import com.cinema.cinema.repository.ShowtimeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

class CinemaManagementServiceTests {
    private final UUID room = UUID.randomUUID();

    @Test
    void acceptsReciprocalCouplePairAndMatchingInactivePair() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        for (boolean active : List.of(true, false)) {
            assertThatCode(() -> CinemaManagementService.validateLayout(room, List.of(
                    seat(first, 1, "COUPLE", active, second), seat(second, 2, "COUPLE", active, first))))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsDuplicateIdsAndPositionsAndWrongAuditorium() {
        UUID first = UUID.randomUUID();
        PhysicalSeatDto seat = seat(first, 1, "STANDARD", true, null);
        for (PhysicalSeatDto invalid : List.of(seat(first, 2, "STANDARD", true, null),
                seat(UUID.randomUUID(), 1, "STANDARD", true, null),
                new PhysicalSeatDto(UUID.randomUUID(), UUID.randomUUID(), "A", 2, "STANDARD", true, null))) {
            assertThatThrownBy(() -> CinemaManagementService.validateLayout(room, List.of(seat, invalid)))
                    .isInstanceOf(CinemaException.class).hasMessageContaining("unique");
        }
    }

    @Test
    void rejectsMissingAsymmetricNonAdjacentAndMixedActivePairs() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PhysicalSeatDto left = seat(first, 1, "COUPLE", true, second);
        for (PhysicalSeatDto invalid : List.of(seat(second, 2, "COUPLE", true, null),
                seat(second, 3, "COUPLE", true, first), seat(second, 2, "COUPLE", false, first),
                seat(second, 2, "STANDARD", true, first))) {
            assertThatThrownBy(() -> CinemaManagementService.validateLayout(room, List.of(left, invalid)))
                    .isInstanceOf(CinemaException.class).hasMessageContaining("reciprocal");
        }
        assertThatThrownBy(() -> CinemaManagementService.validateLayout(room, List.of(left)))
                .isInstanceOf(CinemaException.class);
    }

    @Test
    void anyExistingShowtimeLocksLayoutBeforeAnySeatMutation() {
        AuditoriumRepository auditoriums = mock(AuditoriumRepository.class);
        SeatRepository seats = mock(SeatRepository.class);
        ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
        when(auditoriums.findForUpdateById(room)).thenReturn(Optional.of(new AuditoriumEntity()));
        when(showtimes.existsByAuditoriumId(room)).thenReturn(true);
        CinemaManagementService service = new CinemaManagementService(mock(CinemaRepository.class), auditoriums,
                seats, showtimes, mock(EntityManager.class));
        assertThatThrownBy(() -> service.saveSeats(room, new SaveSeatLayoutRequest(List.of())))
                .isInstanceOf(CinemaException.class).hasMessageContaining("cannot change");
        verifyNoInteractions(seats);
        var order = inOrder(auditoriums, showtimes);
        order.verify(auditoriums).findForUpdateById(room);
        order.verify(showtimes).existsByAuditoriumId(room);
    }

    private PhysicalSeatDto seat(UUID id, int number, String type, boolean active, UUID pair) {
        return new PhysicalSeatDto(id, room, "A", number, type, active, pair);
    }
}
