package com.movieticket.service;

import com.movieticket.dto.TheatreRequest;
import com.movieticket.dto.TheatreResponse;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Theatre;
import com.movieticket.repository.TheatreRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TheatreService {

    private final TheatreRepository theatreRepository;

    public TheatreService(TheatreRepository theatreRepository) {
        this.theatreRepository = theatreRepository;
    }

    public List<TheatreResponse> getAllActiveTheatres() {
        return toResponseList(theatreRepository.findByStatus("ACTIVE"));
    }

    public List<TheatreResponse> getTheatresShowingMovie(Long movieId) {
        return toResponseList(theatreRepository.findTheatresByMovieAndDate(movieId, LocalDate.now()));
    }

    public TheatreResponse getTheatreById(Long id) {
        return TheatreResponse.fromEntity(getTheatreEntity(id));
    }

    public Theatre getTheatreEntity(Long id) {
        Theatre theatre = theatreRepository.findById(id);
        if (theatre == null) {
            throw new ResourceNotFoundException("Theatre not found with id: " + id);
        }
        return theatre;
    }

    public List<TheatreResponse> adminGetAllTheatres() {
        return toResponseList(theatreRepository.findAll());
    }

    public TheatreResponse adminCreateTheatre(TheatreRequest request) {
        validateTheatreRequest(request);
        Theatre theatre = new Theatre(
                request.getName().trim(),
                request.getLocation().trim(),
                request.getAddress() != null ? request.getAddress().trim() : "",
                request.getContactNumber() != null ? request.getContactNumber().trim() : "",
                request.getStatus() != null ? request.getStatus() : "ACTIVE");
        theatre = theatreRepository.save(theatre);
        return TheatreResponse.fromEntity(theatre);
    }

    public TheatreResponse adminUpdateTheatre(Long id, TheatreRequest request) {
        validateTheatreRequest(request);
        Theatre theatre = getTheatreEntity(id);

        theatre.setName(request.getName().trim());
        theatre.setLocation(request.getLocation().trim());
        theatre.setAddress(request.getAddress());
        theatre.setContactNumber(request.getContactNumber());
        if (request.getStatus() != null) {
            theatre.setStatus(request.getStatus());
        }

        theatre = theatreRepository.save(theatre);
        return TheatreResponse.fromEntity(theatre);
    }

    public void adminDeleteTheatre(Long id) {
        Theatre theatre = getTheatreEntity(id);
        theatre.setStatus("INACTIVE");
        theatreRepository.save(theatre);
    }

    private void validateTheatreRequest(TheatreRequest request) {
        if (request == null) {
            throw new BadRequestException("Theatre details are required.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Theatre name is required.");
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new BadRequestException("Location/City is required.");
        }
    }

    private List<TheatreResponse> toResponseList(List<Theatre> theatres) {
        List<TheatreResponse> list = new ArrayList<TheatreResponse>();
        if (theatres == null) {
            return list;
        }
        for (int i = 0; i < theatres.size(); i++) {
            list.add(TheatreResponse.fromEntity(theatres.get(i)));
        }
        return list;
    }
}
