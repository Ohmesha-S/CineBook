package com.movieticket.service;

import com.movieticket.dto.ScreenRequest;
import com.movieticket.dto.ScreenResponse;
import com.movieticket.enums.SeatCategory;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Screen;
import com.movieticket.model.Seat;
import com.movieticket.model.Theatre;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.SeatRepository;
import com.movieticket.repository.TheatreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        return screenRepository.findByTheatreId(theatreId)
                .stream()
                .map(ScreenResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ScreenResponse getScreenById(Long id) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + id));
        return ScreenResponse.fromEntity(screen);
    }

    public Screen getScreenEntity(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + id));
    }

    @Transactional
    public ScreenResponse createScreen(ScreenRequest request) {
        Theatre theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + request.getTheatreId()));

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

        // Auto-generate seat layout in MySQL for this screen
        generateSeatLayout(screen, rows, seatsPerRow);

        return ScreenResponse.fromEntity(screen);
    }

    @Transactional
    public void generateSeatLayout(Screen screen, int totalRows, int seatsPerRow) {
        List<Seat> seats = new ArrayList<>();
        char startRowChar = 'A';

        for (int r = 0; r < totalRows; r++) {
            String rowLabel = String.valueOf((char) (startRowChar + r));
            SeatCategory category;
            double basePrice;

            // Row allocation strategy:
            // Top/Last row -> RECLINER
            // Middle rows -> PREMIUM
            // Front rows -> REGULAR
            if (r == totalRows - 1 && totalRows >= 4) {
                category = SeatCategory.RECLINER;
                basePrice = SeatCategory.RECLINER.getDefaultPrice();
            } else if (r >= (totalRows / 2)) {
                category = SeatCategory.PREMIUM;
                basePrice = SeatCategory.PREMIUM.getDefaultPrice();
            } else {
                category = SeatCategory.REGULAR;
                basePrice = SeatCategory.REGULAR.getDefaultPrice();
            }

            for (int s = 1; s <= seatsPerRow; s++) {
                Seat seat = new Seat(screen, rowLabel, s, category, basePrice);
                seats.add(seat);
            }
        }

        seatRepository.saveAll(seats);
    }

    @Transactional
    public ScreenResponse updateScreen(Long id, ScreenRequest request) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + id));

        screen.setScreenName(request.getScreenName().trim());
        if (request.getScreenType() != null) {
            screen.setScreenType(request.getScreenType());
        }

        screen = screenRepository.save(screen);
        return ScreenResponse.fromEntity(screen);
    }

    @Transactional
    public void deleteScreen(Long id) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + id));
        seatRepository.deleteByScreenId(screen.getId());
        screenRepository.delete(screen);
    }
}
