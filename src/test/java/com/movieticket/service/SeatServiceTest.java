package com.movieticket.service;

import com.movieticket.dto.SeatDto;
import com.movieticket.dto.SeatLayoutResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.SeatCategory;
import com.movieticket.enums.ShowStatus;
import com.movieticket.model.*;
import com.movieticket.repository.BookingSeatRepository;
import com.movieticket.repository.ScreenRepository;
import com.movieticket.repository.SeatRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private ShowSeatReservationRepository reservationRepository;

    @InjectMocks
    private SeatService seatService;

    private Screen screen;
    private Seat seatA1;
    private Seat seatA2;
    private Show show;

    @BeforeEach
    void setUp() {
        Theatre theatre = new Theatre("PVR", "Chennai", "Velachery", "123", "ACTIVE");
        theatre.setId(1L);

        screen = new Screen(theatre, "Screen 1", 5, 50, "IMAX 2D");
        screen.setId(1L);

        seatA1 = new Seat(screen, "A", 1, SeatCategory.REGULAR, 150.0);
        seatA1.setId(101L);

        seatA2 = new Seat(screen, "A", 2, SeatCategory.REGULAR, 150.0);
        seatA2.setId(102L);

        Movie movie = new Movie("Interstellar", "Space", "Sci-Fi", "English", 169, "PG-13",
                LocalDate.now(), "p.jpg", "b.jpg", "Nolan", "Cast", MovieStatus.NOW_SHOWING);
        movie.setId(1L);

        show = new Show(movie, screen, LocalDate.now().plusDays(1), LocalTime.of(14, 0), LocalTime.of(17, 0), ShowStatus.SCHEDULED);
        show.setId(1L);
        show.setPriceMultiplier(1.0);
    }

    @Test
    @DisplayName("Seat 1: Retrieve seat layout with availability")
    void testGetSeatLayoutForShow() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(1L))
                .thenReturn(Arrays.asList(seatA1, seatA2));
        when(reservationRepository.findActiveReservationsByShow(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(bookingSeatRepository.findByShowIdAndStatus(1L, "BOOKED"))
                .thenReturn(Collections.emptyList());

        SeatLayoutResponse response = seatService.getSeatLayoutForShow(1L);

        assertNotNull(response);
        assertEquals(2, response.getSeats().size());
        assertEquals("AVAILABLE", response.getSeats().get(0).getStatus());
        assertEquals("AVAILABLE", response.getSeats().get(1).getStatus());
        assertEquals(150.0, response.getSeats().get(0).getPrice());
    }

    @Test
    @DisplayName("Seat 2: Correctly mark booked seats as BOOKED")
    void testGetSeatLayoutWithBookedSeat() {
        BookingSeat bs = new BookingSeat(null, seatA1, show, 150.0, "BOOKED");

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(1L))
                .thenReturn(Arrays.asList(seatA1, seatA2));
        when(reservationRepository.findActiveReservationsByShow(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(bookingSeatRepository.findByShowIdAndStatus(1L, "BOOKED"))
                .thenReturn(List.of(bs));

        SeatLayoutResponse response = seatService.getSeatLayoutForShow(1L);

        assertNotNull(response);
        assertEquals("BOOKED", response.getSeats().get(0).getStatus());
        assertEquals("AVAILABLE", response.getSeats().get(1).getStatus());
    }

    @Test
    @DisplayName("Seat 3: Calculate seat price with show price multiplier")
    void testSeatPriceCalculationWithMultiplier() {
        show.setPriceMultiplier(1.2); // Weekend multiplier

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(1L))
                .thenReturn(List.of(seatA1));
        when(reservationRepository.findActiveReservationsByShow(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(bookingSeatRepository.findByShowIdAndStatus(1L, "BOOKED"))
                .thenReturn(Collections.emptyList());

        SeatLayoutResponse response = seatService.getSeatLayoutForShow(1L);

        assertEquals(180.0, response.getSeats().get(0).getPrice()); // 150 * 1.2
    }
}
