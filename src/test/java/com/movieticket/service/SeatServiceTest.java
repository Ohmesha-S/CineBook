package com.movieticket.service;

import com.movieticket.dto.SeatLayoutResponse;
import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.SeatCategory;
import com.movieticket.enums.ShowStatus;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.model.Theatre;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        theatre.setId(Long.valueOf(1L));

        screen = new Screen(theatre, "Screen 1", Integer.valueOf(5), Integer.valueOf(50), "IMAX 2D");
        screen.setId(Long.valueOf(1L));

        seatA1 = new Seat(screen, "A", Integer.valueOf(1), SeatCategory.REGULAR, Double.valueOf(150.0));
        seatA1.setId(Long.valueOf(101L));

        seatA2 = new Seat(screen, "A", Integer.valueOf(2), SeatCategory.REGULAR, Double.valueOf(150.0));
        seatA2.setId(Long.valueOf(102L));

        Movie movie = new Movie("Interstellar", "Space", "Sci-Fi", "English", Integer.valueOf(169), "PG-13",
                LocalDate.now(), "p.jpg", "b.jpg", "Nolan", "Cast", MovieStatus.NOW_SHOWING);
        movie.setId(Long.valueOf(1L));

        show = new Show(movie, screen, LocalDate.now().plusDays(1),
                LocalTime.of(14, 0), LocalTime.of(17, 0), ShowStatus.SCHEDULED);
        show.setId(Long.valueOf(1L));
        show.setPriceMultiplier(Double.valueOf(1.0));
    }

    @Test
    @DisplayName("Seat 1: Retrieve seat layout with availability")
    void testGetSeatLayoutForShow() {
        when(showRepository.findById(Long.valueOf(1L))).thenReturn(show);
        when(seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Long.valueOf(1L)))
                .thenReturn(Arrays.asList(seatA2, seatA1));
        when(reservationRepository.findActiveReservationsByShow(eq(Long.valueOf(1L)), any(LocalDateTime.class)))
                .thenReturn(Collections.<ShowSeatReservation>emptyList());
        when(bookingSeatRepository.findByShowIdAndStatus(Long.valueOf(1L), "BOOKED"))
                .thenReturn(Collections.<BookingSeat>emptyList());

        SeatLayoutResponse response = seatService.getSeatLayoutForShow(Long.valueOf(1L));

        assertNotNull(response);
        assertEquals(2, response.getSeats().size());
        assertEquals(Integer.valueOf(1), response.getSeats().get(0).getSeatNumber());
        assertEquals("AVAILABLE", response.getSeats().get(0).getStatus());
        assertEquals("AVAILABLE", response.getSeats().get(1).getStatus());
        assertEquals(Double.valueOf(150.0), response.getSeats().get(0).getPrice());
    }

    @Test
    @DisplayName("Seat 2: Correctly mark booked seats as BOOKED")
    void testGetSeatLayoutWithBookedSeat() {
        BookingSeat bs = new BookingSeat(null, seatA1, show, Double.valueOf(150.0), "BOOKED");

        when(showRepository.findById(Long.valueOf(1L))).thenReturn(show);
        when(seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Long.valueOf(1L)))
                .thenReturn(Arrays.asList(seatA1, seatA2));
        when(reservationRepository.findActiveReservationsByShow(eq(Long.valueOf(1L)), any(LocalDateTime.class)))
                .thenReturn(Collections.<ShowSeatReservation>emptyList());
        when(bookingSeatRepository.findByShowIdAndStatus(Long.valueOf(1L), "BOOKED"))
                .thenReturn(Arrays.asList(bs));

        SeatLayoutResponse response = seatService.getSeatLayoutForShow(Long.valueOf(1L));

        assertNotNull(response);
        assertEquals("BOOKED", response.getSeats().get(0).getStatus());
        assertEquals("AVAILABLE", response.getSeats().get(1).getStatus());
    }

    @Test
    @DisplayName("Seat 3: Calculate seat price with show price multiplier")
    void testSeatPriceCalculationWithMultiplier() {
        show.setPriceMultiplier(Double.valueOf(1.2));

        when(showRepository.findById(Long.valueOf(1L))).thenReturn(show);
        when(seatRepository.findByScreenIdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Long.valueOf(1L)))
                .thenReturn(Arrays.asList(seatA1));
        when(reservationRepository.findActiveReservationsByShow(eq(Long.valueOf(1L)), any(LocalDateTime.class)))
                .thenReturn(Collections.<ShowSeatReservation>emptyList());
        when(bookingSeatRepository.findByShowIdAndStatus(Long.valueOf(1L), "BOOKED"))
                .thenReturn(Collections.<BookingSeat>emptyList());

        SeatLayoutResponse response = seatService.getSeatLayoutForShow(Long.valueOf(1L));

        assertEquals(Double.valueOf(180.0), response.getSeats().get(0).getPrice());
    }
}
