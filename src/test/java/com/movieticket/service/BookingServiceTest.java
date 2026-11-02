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
import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Movie;
import com.movieticket.model.Screen;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.model.Theatre;
import com.movieticket.model.User;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.BookingSeatRepository;
import com.movieticket.repository.SeatRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private BookingService bookingService;

    private User customer;
    private Show show;
    private Seat seatA1;
    private Seat seatA2;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, bookingSeatRepository, reservationRepository,
                showRepository, seatRepository, authService);

        customer = new User("Rahul", "rahul@cinebook.com", "123", "pass", UserRole.CUSTOMER);
        customer.setId(Long.valueOf(1L));

        Movie movie = new Movie("Interstellar", "Space", "Sci-Fi", "English", Integer.valueOf(169), "PG-13",
                LocalDate.now(), "p.jpg", "b.jpg", "Nolan", "Cast", MovieStatus.NOW_SHOWING);
        movie.setId(Long.valueOf(1L));

        Theatre theatre = new Theatre("PVR", "Chennai", "Velachery", "123", "ACTIVE");
        theatre.setId(Long.valueOf(1L));

        Screen screen = new Screen(theatre, "Screen 1", Integer.valueOf(5), Integer.valueOf(50), "IMAX 2D");
        screen.setId(Long.valueOf(1L));

        seatA1 = new Seat(screen, "A", Integer.valueOf(1), SeatCategory.REGULAR, Double.valueOf(150.0));
        seatA1.setId(Long.valueOf(101L));

        seatA2 = new Seat(screen, "A", Integer.valueOf(2), SeatCategory.REGULAR, Double.valueOf(150.0));
        seatA2.setId(Long.valueOf(102L));

        show = new Show(movie, screen, LocalDate.now().plusDays(1),
                LocalTime.of(14, 0), LocalTime.of(17, 0), ShowStatus.SCHEDULED);
        show.setId(Long.valueOf(1L));
        show.setPriceMultiplier(Double.valueOf(1.0));
    }

    @Test
    @DisplayName("Booking 1: Successful multiple-seat booking with correct total calculation")
    void testSuccessfulBooking() {
        List<Long> ids = Arrays.asList(Long.valueOf(101L), Long.valueOf(102L));
        BookingRequest req = new BookingRequest(Long.valueOf(1L), ids);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(Long.valueOf(1L))).thenReturn(show);
        when(seatRepository.findByIdIn(anyList())).thenReturn(new ArrayList<Seat>(Arrays.asList(seatA1, seatA2)));
        when(Boolean.valueOf(bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(
                eq(Long.valueOf(1L)), any(Long.class), eq("BOOKED")))).thenReturn(Boolean.FALSE);
        when(reservationRepository.findActiveReservationsByShow(eq(Long.valueOf(1L)), any(LocalDateTime.class)))
                .thenReturn(Collections.<ShowSeatReservation>emptyList());

        Booking savedBooking = new Booking("CB-20261030-ABCDE", customer, show, Double.valueOf(320.0), BookingStatus.PENDING_PAYMENT);
        savedBooking.setId(Long.valueOf(10L));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingSeatRepository.save(any(BookingSeat.class))).thenAnswer(new Answer<BookingSeat>() {
            @Override
            public BookingSeat answer(InvocationOnMock invocation) {
                return (BookingSeat) invocation.getArgument(0);
            }
        });

        BookingResponse response = bookingService.createBooking(req);

        assertNotNull(response);
        assertEquals(BookingStatus.PENDING_PAYMENT, response.getBookingStatus());
        assertEquals(Double.valueOf(320.0), response.getTotalAmount());
        verify(reservationRepository, times(2)).save(any(ShowSeatReservation.class));
        verify(bookingSeatRepository, times(2)).save(any(BookingSeat.class));
    }

    @Test
    @DisplayName("Booking 2: Reject already-booked seat (Concurrency prevention)")
    void testRejectAlreadyBookedSeat() {
        List<Long> ids = Arrays.asList(Long.valueOf(101L));
        BookingRequest req = new BookingRequest(Long.valueOf(1L), ids);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(Long.valueOf(1L))).thenReturn(show);
        when(seatRepository.findByIdIn(anyList())).thenReturn(new ArrayList<Seat>(Arrays.asList(seatA1)));
        when(Boolean.valueOf(bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(
                Long.valueOf(1L), Long.valueOf(101L), "BOOKED"))).thenReturn(Boolean.TRUE);

        try {
            bookingService.createBooking(req);
            fail("Expected BookingConflictException");
        } catch (BookingConflictException expected) {
            assertNotNull(expected.getMessage());
        }
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Booking 3: Reject seat currently held by another user")
    void testRejectSeatHeldByAnotherUser() {
        List<Long> ids = Arrays.asList(Long.valueOf(101L));
        BookingRequest req = new BookingRequest(Long.valueOf(1L), ids);

        ShowSeatReservation existingHold = new ShowSeatReservation(
                show, seatA1, customer, "CB-OTHER", "HELD", LocalDateTime.now().plusMinutes(5));

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(Long.valueOf(1L))).thenReturn(show);
        when(seatRepository.findByIdIn(anyList())).thenReturn(new ArrayList<Seat>(Arrays.asList(seatA1)));
        when(Boolean.valueOf(bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(
                Long.valueOf(1L), Long.valueOf(101L), "BOOKED"))).thenReturn(Boolean.FALSE);
        when(reservationRepository.findActiveReservationsByShow(eq(Long.valueOf(1L)), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(existingHold));

        try {
            bookingService.createBooking(req);
            fail("Expected BookingConflictException");
        } catch (BookingConflictException expected) {
            assertNotNull(expected.getMessage());
        }
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Booking 4: Reject booking for past show")
    void testRejectBookingForPastShow() {
        show.setShowDate(LocalDate.now().minusDays(1));
        List<Long> ids = Arrays.asList(Long.valueOf(101L));
        BookingRequest req = new BookingRequest(Long.valueOf(1L), ids);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(customer);
        when(showRepository.findById(Long.valueOf(1L))).thenReturn(show);

        try {
            bookingService.createBooking(req);
            fail("Expected BadRequestException");
        } catch (BadRequestException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
