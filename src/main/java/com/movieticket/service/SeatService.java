package com.movieticket.service;

import com.movieticket.dto.SeatDto;
import com.movieticket.dto.SeatLayoutResponse;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Screen;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.repository.BookingSeatRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.SeatRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final ShowRepository showRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowSeatReservationRepository reservationRepository;

    public SeatService(SeatRepository seatRepository,
                       ScreenRepository screenRepository,
                       ShowRepository showRepository,
                       BookingSeatRepository bookingSeatRepository,
                       ShowSeatReservationRepository reservationRepository) {
        this.seatRepository = seatRepository;
        this.screenRepository = screenRepository;
        this.showRepository = showRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<SeatDto> getSeatsByScreen(Long screenId) {
        List<Seat> seats = seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(screenId);
        return seats.stream()
                .map(s -> SeatDto.fromEntity(s, s.getBasePrice(), "AVAILABLE"))
                .collect(Collectors.toList());
    }

    @Transactional
    public SeatLayoutResponse getSeatLayoutForShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + showId));

        Screen screen = show.getScreen();
        List<Seat> seats = seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(screen.getId());

        // Clean up expired holds in reservation table
        reservationRepository.cleanupExpiredHolds(LocalDateTime.now());

        // Get currently active reservations (HELD or CONFIRMED)
        List<ShowSeatReservation> activeReservations = reservationRepository.findActiveReservationsByShow(showId, LocalDateTime.now());
        Set<Long> heldOrConfirmedSeatIds = activeReservations.stream()
                .map(r -> r.getSeat().getId())
                .collect(Collectors.toSet());

        // Also check booking_seats table for confirmed bookings
        Set<Long> confirmedBookingSeatIds = bookingSeatRepository.findByShowIdAndStatus(showId, "BOOKED")
                .stream()
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toSet());

        heldOrConfirmedSeatIds.addAll(confirmedBookingSeatIds);

        double multiplier = show.getPriceMultiplier() != null ? show.getPriceMultiplier() : 1.0;

        List<SeatDto> dtoList = new ArrayList<>();
        Map<String, List<SeatDto>> rowsMap = new LinkedHashMap<>();

        for (Seat seat : seats) {
            double effectivePrice = Math.round(seat.getBasePrice() * multiplier * 100.0) / 100.0;
            String status = heldOrConfirmedSeatIds.contains(seat.getId()) ? "BOOKED" : "AVAILABLE";

            SeatDto dto = SeatDto.fromEntity(seat, effectivePrice, status);
            dtoList.add(dto);

            rowsMap.computeIfAbsent(seat.getRowLabel(), k -> new ArrayList<>()).add(dto);
        }

        String theatreName = (screen.getTheatre() != null) ? screen.getTheatre().getName() : "";

        return new SeatLayoutResponse(
                screen.getId(),
                screen.getScreenName(),
                theatreName,
                showId,
                screen.getTotalRows(),
                screen.getTotalSeats(),
                dtoList,
                rowsMap
        );
    }
}
