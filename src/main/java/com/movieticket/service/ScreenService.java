package com.movieticket.service;

import com.movieticket.dto.ScreenRequest;
import com.movieticket.dto.ScreenResponse;
import com.movieticket.enums.SeatCategory;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Screen;
import com.movieticket.model.Seat;
import com.movieticket.model.Theatre;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.SeatRepository;
import com.movieticket.repository.TheatreRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;

    public ScreenService(ScreenRepository screenRepository,
                         TheatreRepository theatreRepository,
                         SeatRepository seatRepository) {
        this.screenRepository = screenRepository;
        this.theatreRepository = theatreRepository;
        this.seatRepository = seatRepository;
    }

    public List<ScreenResponse> getScreensByTheatre(Long theatreId) {
        List<Screen> screens = screenRepository.findByTheatreId(theatreId);
        List<ScreenResponse> responses = new ArrayList<ScreenResponse>();
        for (int i = 0; i < screens.size(); i++) {
            responses.add(ScreenResponse.fromEntity(screens.get(i)));
        }
        return responses;
    }

    public ScreenResponse getScreenById(Long id) {
        return ScreenResponse.fromEntity(getScreenEntity(id));
    }

    public Screen getScreenEntity(Long id) {
        Screen screen = screenRepository.findById(id);
        if (screen == null) {
            throw new ResourceNotFoundException("Screen not found with id: " + id);
        }
        return screen;
    }

    public ScreenResponse createScreen(ScreenRequest request) {
        if (request == null || request.getTheatreId() == null) {
            throw new BadRequestException("Theatre ID is required.");
        }
        if (request.getScreenName() == null || request.getScreenName().trim().isEmpty()) {
            throw new BadRequestException("Screen name is required.");
        }

        Theatre theatre = theatreRepository.findById(request.getTheatreId());
        if (theatre == null) {
            throw new ResourceNotFoundException("Theatre not found with id: " + request.getTheatreId());
        }

        int rows = request.getTotalRows() != null && request.getTotalRows() > 0 ? request.getTotalRows() : 5;
        int seatsPerRow = request.getSeatsPerRow() != null && request.getSeatsPerRow() > 0 ? request.getSeatsPerRow() : 10;
        int totalSeats = rows * seatsPerRow;

        Screen screen = new Screen(
                theatre,
                request.getScreenName().trim(),
                rows,
                totalSeats,
                request.getScreenType() != null ? request.getScreenType() : "Dolby Atmos 4K"
        );
        screen = screenRepository.save(screen);

        generateSeatLayout(screen, rows, seatsPerRow);

        return ScreenResponse.fromEntity(screen);
    }

    public void generateSeatLayout(Screen screen, int totalRows, int seatsPerRow) {
        List<Seat> seats = new ArrayList<Seat>();
        char startRowChar = 'A';

        for (int r = 0; r < totalRows; r++) {
            String rowLabel = String.valueOf((char) (startRowChar + r));
            String category;

            if (r == totalRows - 1 && totalRows >= 4) {
                category = SeatCategory.RECLINER;
            } else if (r >= (totalRows / 2)) {
                category = SeatCategory.PREMIUM;
            } else {
                category = SeatCategory.REGULAR;
            }
            double basePrice = SeatCategory.getDefaultPrice(category);

            for (int s = 1; s <= seatsPerRow; s++) {
                Seat seat = new Seat(screen, rowLabel, Integer.valueOf(s), category, Double.valueOf(basePrice));
                seats.add(seat);
            }
        }

        seatRepository.saveAll(seats);
    }

    public ScreenResponse updateScreen(Long id, ScreenRequest request) {
        if (request == null || request.getScreenName() == null || request.getScreenName().trim().isEmpty()) {
            throw new BadRequestException("Screen name is required.");
        }
        Screen screen = getScreenEntity(id);
        screen.setScreenName(request.getScreenName().trim());
        if (request.getScreenType() != null) {
            screen.setScreenType(request.getScreenType());
        }

        screen = screenRepository.save(screen);
        return ScreenResponse.fromEntity(screen);
    }

    public void deleteScreen(Long id) {
        Screen screen = getScreenEntity(id);
        seatRepository.deleteByScreenId(screen.getId());
        screenRepository.deleteById(screen.getId());
    }
}
