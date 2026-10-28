package com.cinema.cinema.controller;

import java.util.List;
import java.util.UUID;
import com.cinema.cinema.dto.AdminAuditoriumResponse;
import com.cinema.cinema.dto.CinemaResponse;
import com.cinema.cinema.dto.PhysicalSeatDto;
import com.cinema.cinema.dto.SaveAuditoriumRequest;
import com.cinema.cinema.dto.SaveCinemaRequest;
import com.cinema.cinema.dto.SaveSeatLayoutRequest;
import com.cinema.cinema.service.CinemaManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class CinemaAdminController {
    private final CinemaManagementService management;

    public CinemaAdminController(CinemaManagementService management) {
        this.management = management;
    }

    @GetMapping("/cinemas")
    public List<CinemaResponse> cinemas() {
        return management.listCinemas();
    }

    @PostMapping("/cinemas")
    @ResponseStatus(HttpStatus.CREATED)
    public CinemaResponse createCinema(@Valid @RequestBody SaveCinemaRequest request) {
        return management.saveCinema(null, request);
    }

    @PutMapping("/cinemas/{id}")
    public CinemaResponse updateCinema(@PathVariable UUID id, @Valid @RequestBody SaveCinemaRequest request) {
        return management.saveCinema(id, request);
    }

    @GetMapping("/cinemas/{id}/auditoriums")
    public List<AdminAuditoriumResponse> auditoriums(@PathVariable UUID id) {
        return management.listAuditoriums(id);
    }

    @PostMapping("/cinemas/{id}/auditoriums")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAuditoriumResponse createAuditorium(@PathVariable UUID id, @Valid @RequestBody SaveAuditoriumRequest request) {
        return management.createAuditorium(id, request);
    }

    @PutMapping("/auditoriums/{id}")
    public AdminAuditoriumResponse renameAuditorium(@PathVariable UUID id, @Valid @RequestBody SaveAuditoriumRequest request) {
        return management.renameAuditorium(id, request);
    }

    @GetMapping("/auditoriums/{id}/seats")
    public List<PhysicalSeatDto> seats(@PathVariable UUID id) {
        return management.getSeats(id);
    }

    @PutMapping("/auditoriums/{id}/seats")
    public List<PhysicalSeatDto> saveSeats(@PathVariable UUID id, @Valid @RequestBody SaveSeatLayoutRequest request) {
        return management.saveSeats(id, request);
    }
}
