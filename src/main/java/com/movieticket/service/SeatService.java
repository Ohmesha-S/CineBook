package com.movieticket.service;

import com.movieticket.dto.SeatDto;
import com.movieticket.dto.SeatLayoutResponse;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Screen;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.repository.BookingSeatRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.SeatRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import com.movieticket.util.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Unit 2: Java Collections - ArrayList, HashSet, TreeMap (sorted row map), and Comparable<Seat>.
 */
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
        CollectionUtils.sortNaturally(seats);
        List<SeatDto> dtos = new ArrayList<SeatDto>();
        for (int i = 0; i < seats.size(); i++) {
            Seat s = seats.get(i);
            dtos.add(SeatDto.fromEntity(s, s.getBasePrice(), "AVAILABLE"));
        }
        return dtos;
    }

    public SeatLayoutResponse getSeatLayoutForShow(Long showId) {
        Show show = showRepository.findById(showId);
        if (show == null) {
            throw new ResourceNotFoundException("Show not found with id: " + showId);
        }

        Screen screen = show.getScreen();
        if (screen == null && screenRepository != null) {
            throw new ResourceNotFoundException("Screen not found for show id: " + showId);
        }

        List<Seat> seats = new ArrayList<Seat>(
                seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(screen.getId()));
        CollectionUtils.sortNaturally(seats);

        LocalDateTime now = LocalDateTime.now();
        reservationRepository.cleanupExpiredHolds(now);

        Set<Long> occupiedSeatIds = new HashSet<Long>();

        List<ShowSeatReservation> activeReservations = reservationRepository.findActiveReservationsByShow(showId, now);
        for (int i = 0; i < activeReservations.size(); i++) {
            ShowSeatReservation r = activeReservations.get(i);
            if (r.getSeat() != null && r.getSeat().getId() != null) {
                occupiedSeatIds.add(r.getSeat().getId());
            }
        }

        List<BookingSeat> confirmedBookingSeats = bookingSeatRepository.findByShowIdAndStatus(showId, "BOOKED");
        for (int i = 0; i < confirmedBookingSeats.size(); i++) {
            BookingSeat bs = confirmedBookingSeats.get(i);
            if (bs.getSeat() != null && bs.getSeat().getId() != null) {
                occupiedSeatIds.add(bs.getSeat().getId());
            }
        }

        double multiplier = show.getPriceMultiplier() != null ? show.getPriceMultiplier() : 1.0;

        List<SeatDto> dtoList = new ArrayList<SeatDto>();
        Map<String, List<SeatDto>> rowsMap = new TreeMap<String, List<SeatDto>>();

        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            double effectivePrice = Math.round(seat.getBasePrice() * multiplier * 100.0) / 100.0;
            String status = occupiedSeatIds.contains(seat.getId()) ? "BOOKED" : "AVAILABLE";

            SeatDto dto = SeatDto.fromEntity(seat, Double.valueOf(effectivePrice), status);
            dtoList.add(dto);

            List<SeatDto> rowSeats = rowsMap.get(seat.getRowLabel());
            if (rowSeats == null) {
                rowSeats = new ArrayList<SeatDto>();
                rowsMap.put(seat.getRowLabel(), rowSeats);
            }
            rowSeats.add(dto);
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
