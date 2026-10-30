package com.movieticket.service;

import com.movieticket.dto.BookingRequest;
import com.movieticket.dto.BookingResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.MovieStatus;
import com.movieticket.enums.SeatCategory;
import com.movieticket.enums.ShowStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.BookingConflictException;
import com.movieticket.model.*;
import com.movieticket.repository.*;
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
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private ShowSeatReservationRepository reservationRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private BookingService bookingService;

    private User customer;
    private Show show;
    private Seat seatA1;
    private Seat seatA2;

    @BeforeEach
    void setUp() {
        customer = new User("Rahul", "rahul@cinebook.com", "123", "pass", UserRole.CUSTOMER);
        customer.setId(1L);

        Movie movie = new Movie("Interstellar", "Space", "Sci-Fi", "English", 169, "PG-13",
                LocalDate.now(), "p.jpg", "b.jpg", "Nolan", "Cast", MovieStatus.NOW_SHOWING);
        movie.setId(1L);

        Theatre theatre = new Theatre("PVR", "Chennai", "Velachery", "123", "ACTIVE");
        theatre.setId(1L);

        Screen screen = new Screen(theatre, "Screen 1", 5, 50, "IMAX 2D");
        screen.setId(1L);

        seatA1 = new Seat(screen, "A", 1, SeatCategory.REGULAR, 150.0);
        seatA1.setId(101L);

        seatA2 = new Seat(screen, "A", 2, SeatCategory.REGULAR, 150.0);
        seatA2.setId(102L);

        show = new Show(movie, screen, LocalDate.now().plusDays(1), LocalTime.of(14, 0), LocalTime.of(17, 0), ShowStatus.SCHEDULED);
        show.setId(1L);
        show.setPriceMultiplier(1.0);
    }

    @Test
    @DisplayName("Booking 1: Successful multiple-seat booking with correct total calculation")
    void testSuccessfulBooking() {
        BookingRequest req = new BookingRequest(1L, Arrays.asList(101L, 102L));

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByIdIn(req.getSeatIds())).thenReturn(Arrays.asList(seatA1, seatA2));
        when(bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(eq(1L), any(), eq("BOOKED"))).thenReturn(false);
        when(reservationRepository.findActiveReservationsByShow(eq(1L), any())).thenReturn(Collections.emptyList());

        Booking savedBooking = new Booking("CB-20261030-ABCDE", customer, show, 320.0, BookingStatus.PENDING_PAYMENT);
        savedBooking.setId(10L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(req);

        assertNotNull(response);
        assertEquals(BookingStatus.PENDING_PAYMENT, response.getBookingStatus());
        assertEquals(320.0, response.getTotalAmount()); // 150 + 150 + 20 convenience fee
        verify(reservationRepository, times(1)).saveAll(any());
        verify(bookingSeatRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Booking 2: Reject already-booked seat (Concurrency prevention)")
    void testRejectAlreadyBookedSeat() {
        BookingRequest req = new BookingRequest(1L, List.of(101L));

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByIdIn(req.getSeatIds())).thenReturn(List.of(seatA1));
        when(bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(1L, 101L, "BOOKED")).thenReturn(true);

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(req));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Booking 3: Reject seat currently held by another user")
    void testRejectSeatHeldByAnotherUser() {
        BookingRequest req = new BookingRequest(1L, List.of(101L));

        ShowSeatReservation existingHold = new ShowSeatReservation(show, seatA1, customer, "CB-OTHER", "HELD", LocalDateTime.now().plusMinutes(5));

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(seatRepository.findByIdIn(req.getSeatIds())).thenReturn(List.of(seatA1));
        when(bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(1L, 101L, "BOOKED")).thenReturn(false);
        when(reservationRepository.findActiveReservationsByShow(eq(1L), any())).thenReturn(List.of(existingHold));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(req));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Booking 4: Reject booking for past show")
    void testRejectBookingForPastShow() {
        show.setShowDate(LocalDate.now().minusDays(1));
        BookingRequest req = new BookingRequest(1L, List.of(101L));

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(req));
    }
}
