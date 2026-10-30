package com.movieticket.service;

import com.movieticket.dto.TheatreRequest;
import com.movieticket.dto.TheatreResponse;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Theatre;
import com.movieticket.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TheatreService {

    private final TheatreRepository theatreRepository;

    public TheatreService(TheatreRepository theatreRepository) {
        this.theatreRepository = theatreRepository;
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> getAllActiveTheatres() {
        return theatreRepository.findByStatus("ACTIVE")
                .stream()
                .map(TheatreResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> getTheatresShowingMovie(Long movieId) {
        return theatreRepository.findTheatresByMovieAndDate(movieId, LocalDate.now())
                .stream()
                .map(TheatreResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(Long id) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));
        return TheatreResponse.fromEntity(theatre);
    }

    public Theatre getTheatreEntity(Long id) {
        return theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));
    }

    // Admin Operations
    public List<TheatreResponse> adminGetAllTheatres() {
        return theatreRepository.findAll()
                .stream()
                .map(TheatreResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public TheatreResponse adminCreateTheatre(TheatreRequest request) {
        Theatre theatre = new Theatre(
                request.getName().trim(),
                request.getLocation().trim(),
                request.getAddress() != null ? request.getAddress().trim() : "",
                request.getContactNumber() != null ? request.getContactNumber().trim() : "",
                request.getStatus() != null ? request.getStatus() : "ACTIVE"
        );
        theatre = theatreRepository.save(theatre);
        return TheatreResponse.fromEntity(theatre);
    }

    @Transactional
    public TheatreResponse adminUpdateTheatre(Long id, TheatreRequest request) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));

        theatre.setName(request.getName().trim());
        theatre.setLocation(request.getLocation().trim());
        theatre.setAddress(request.getAddress());
        theatre.setContactNumber(request.getContactNumber());
        if (request.getStatus() != null) theatre.setStatus(request.getStatus());

        theatre = theatreRepository.save(theatre);
        return TheatreResponse.fromEntity(theatre);
    }

    @Transactional
    public void adminDeleteTheatre(Long id) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));
        theatre.setStatus("INACTIVE");
        theatreRepository.save(theatre);
    }
}
