package com.cinema.cinema.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.cinema.cinema.dto.AdminAuditoriumResponse;
import com.cinema.cinema.dto.CinemaResponse;
import com.cinema.cinema.dto.PhysicalSeatDto;
import com.cinema.cinema.dto.SaveAuditoriumRequest;
import com.cinema.cinema.dto.SaveCinemaRequest;
import com.cinema.cinema.dto.SaveSeatLayoutRequest;
import com.cinema.cinema.entity.AuditoriumEntity;
import com.cinema.cinema.entity.CinemaEntity;
import com.cinema.cinema.entity.SeatEntity;
import com.cinema.cinema.exception.CinemaException;
import com.cinema.cinema.repository.AuditoriumRepository;
import com.cinema.cinema.repository.CinemaRepository;
import com.cinema.cinema.repository.SeatRepository;
import com.cinema.cinema.repository.ShowtimeRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CinemaManagementService {
    private final CinemaRepository cinemas;
    private final AuditoriumRepository auditoriums;
    private final SeatRepository seats;
    private final ShowtimeRepository showtimes;
    private final EntityManager entityManager;

    public CinemaManagementService(CinemaRepository cinemas, AuditoriumRepository auditoriums,
                                   SeatRepository seats, ShowtimeRepository showtimes, EntityManager entityManager) {
        this.cinemas = cinemas;
        this.auditoriums = auditoriums;
        this.seats = seats;
        this.showtimes = showtimes;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<CinemaResponse> listCinemas() {
        return cinemas.findAllByOrderByNameAsc().stream().map(CinemaManagementService::cinemaResponse).toList();
    }

    @Transactional
    public CinemaResponse saveCinema(UUID id, SaveCinemaRequest request) {
        String timezone = request.timezone() == null ? "Asia/Ho_Chi_Minh" : request.timezone().trim();
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw invalid("INVALID_TIMEZONE", "Timezone must be a valid IANA timezone.");
        }
        Instant now = Instant.now();
        CinemaEntity cinema = id == null
                ? new CinemaEntity(UUID.randomUUID(), request.name().trim(), request.address().trim(),
                    request.city().trim(), timezone, now)
                : requireCinema(id);
        cinema.update(request.name().trim(), request.address().trim(), request.city().trim(), timezone, now);
        return cinemaResponse(cinemas.saveAndFlush(cinema));
    }

    @Transactional(readOnly = true)
    public List<AdminAuditoriumResponse> listAuditoriums(UUID cinemaId) {
        requireCinema(cinemaId);
        return auditoriums.findByCinemaIdOrderByNameAsc(cinemaId).stream()
                .map(CinemaManagementService::auditoriumResponse).toList();
    }

    @Transactional
    public AdminAuditoriumResponse createAuditorium(UUID cinemaId, SaveAuditoriumRequest request) {
        requireCinema(cinemaId);
        return auditoriumResponse(auditoriums.saveAndFlush(
                new AuditoriumEntity(UUID.randomUUID(), cinemaId, request.name().trim(), Instant.now())));
    }

    @Transactional
    public AdminAuditoriumResponse renameAuditorium(UUID id, SaveAuditoriumRequest request) {
        AuditoriumEntity auditorium = lockAuditorium(id);
        auditorium.rename(request.name().trim(), Instant.now());
        return auditoriumResponse(auditoriums.saveAndFlush(auditorium));
    }

    @Transactional(readOnly = true)
    public List<PhysicalSeatDto> getSeats(UUID auditoriumId) {
        auditoriums.findById(auditoriumId).orElseThrow(() -> notFound("AUDITORIUM_NOT_FOUND"));
        return seats.findByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(auditoriumId).stream()
                .map(CinemaManagementService::seatResponse).toList();
    }

    @Transactional
    public List<PhysicalSeatDto> saveSeats(UUID auditoriumId, SaveSeatLayoutRequest request) {
        // Serialize layout replacement with showtime creation/publication on the same room row.
        lockAuditorium(auditoriumId);
        if (showtimes.existsByAuditoriumId(auditoriumId)) {
            throw new CinemaException(HttpStatus.CONFLICT, "LAYOUT_LOCKED",
                    "Seat layout cannot change after a showtime has been created for this auditorium.");
        }
        validateLayout(auditoriumId, request.seats());
        List<SeatEntity> existing = seats.findAllById(request.seats().stream().map(PhysicalSeatDto::id).toList());
        if (existing.stream().anyMatch(seat -> !seat.getAuditoriumId().equals(auditoriumId))) {
            throw invalid("INVALID_SEAT", "Seat IDs cannot belong to another auditorium.");
        }
        seats.deleteLayout(auditoriumId);
        // Bulk deletion bypasses the persistence context; reused seat IDs must be inserted anew.
        entityManager.clear();
        Instant now = Instant.now();
        // Insert only: a concurrent layout must never turn a shared UUID into a cross-room merge.
        request.seats().forEach(seat -> entityManager.persist(new SeatEntity(seat.id(), auditoriumId,
                seat.rowLabel(), seat.seatNumber(), seat.seatType(), seat.isActive(), seat.pairedSeatId(), now)));
        seats.flush();
        return getSeats(auditoriumId);
    }

    static void validateLayout(UUID auditoriumId, List<PhysicalSeatDto> layout) {
        Map<UUID, PhysicalSeatDto> byId = new HashMap<>();
        Set<String> positions = new HashSet<>();
        for (PhysicalSeatDto seat : layout) {
            if (!auditoriumId.equals(seat.auditoriumId()) || byId.put(seat.id(), seat) != null
                    || !positions.add(seat.rowLabel() + ":" + seat.seatNumber())) {
                throw invalid("INVALID_SEAT", "Seat IDs and positions must be unique and belong to this auditorium.");
            }
        }
        for (PhysicalSeatDto seat : layout) {
            PhysicalSeatDto partner = byId.get(seat.pairedSeatId());
            if ("COUPLE".equals(seat.seatType()) ? partner == null || partner.id().equals(seat.id())
                    || !"COUPLE".equals(partner.seatType()) || !seat.id().equals(partner.pairedSeatId())
                    || !seat.rowLabel().equals(partner.rowLabel())
                    || Math.abs(seat.seatNumber() - partner.seatNumber()) != 1
                    || !seat.isActive().equals(partner.isActive()) : seat.pairedSeatId() != null) {
                throw invalid("INVALID_COUPLE", "Couple seats must form reciprocal adjacent pairs with matching active state.");
            }
        }
    }

    private CinemaEntity requireCinema(UUID id) {
        return cinemas.findById(id).orElseThrow(() -> notFound("CINEMA_NOT_FOUND"));
    }

    private AuditoriumEntity lockAuditorium(UUID id) {
        return auditoriums.findForUpdateById(id).orElseThrow(() -> notFound("AUDITORIUM_NOT_FOUND"));
    }

    private static CinemaResponse cinemaResponse(CinemaEntity cinema) {
        return new CinemaResponse(cinema.getId().toString(), cinema.getName(), cinema.getAddress(), cinema.getCity(), cinema.getTimezone());
    }

    private static AdminAuditoriumResponse auditoriumResponse(AuditoriumEntity auditorium) {
        return new AdminAuditoriumResponse(auditorium.getId(), auditorium.getCinemaId(), auditorium.getName());
    }

    private static PhysicalSeatDto seatResponse(SeatEntity seat) {
        return new PhysicalSeatDto(seat.getId(), seat.getAuditoriumId(), seat.getRowLabel(), seat.getSeatNumber(),
                seat.getSeatType(), seat.getIsActive(), seat.getPairedSeatId());
    }

    private static CinemaException invalid(String code, String message) {
        return new CinemaException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static CinemaException notFound(String code) {
        return new CinemaException(HttpStatus.NOT_FOUND, code, "The requested cinema or auditorium was not found.");
    }
}
