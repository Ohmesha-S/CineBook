package com.movieticket.service;

import com.movieticket.dto.BookingResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.UnauthorizedException;
import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CancellationServiceTest {

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

    private User owner;
    private User otherUser;
    private Show futureShow;
    private Booking confirmedBooking;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, bookingSeatRepository, reservationRepository,
                showRepository, seatRepository, authService);

        owner = new User("Rahul", "rahul@cinebook.com", "123", "pass", UserRole.CUSTOMER);
        owner.setId(Long.valueOf(1L));

        otherUser = new User("Other", "other@cinebook.com", "456", "pass", UserRole.CUSTOMER);
        otherUser.setId(Long.valueOf(2L));

        futureShow = new Show(null, null, LocalDate.now().plusDays(2),
                LocalTime.of(18, 0), LocalTime.of(21, 0), ShowStatus.SCHEDULED);
        futureShow.setId(Long.valueOf(1L));

        confirmedBooking = new Booking("CB-20261030-CONF", owner, futureShow, Double.valueOf(200.0), BookingStatus.CONFIRMED);
        confirmedBooking.setId(Long.valueOf(10L));

        Seat seat1 = new Seat(null, "B", Integer.valueOf(4), null, Double.valueOf(180.0));
        seat1.setId(Long.valueOf(55L));
        BookingSeat bs = new BookingSeat(confirmedBooking, seat1, futureShow, Double.valueOf(180.0), "BOOKED");
        List<BookingSeat> seats = new ArrayList<BookingSeat>();
        seats.add(bs);
        confirmedBooking.setBookingSeats(seats);
    }

    @Test
    @DisplayName("Cancel 1: Successful cancellation releases seats")
    void testSuccessfulCancellation() {
        when(bookingRepository.findById(Long.valueOf(10L))).thenReturn(confirmedBooking);
        when(authService.getCurrentAuthenticatedUser()).thenReturn(owner);
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);

        BookingResponse response = bookingService.cancelBooking(Long.valueOf(10L));

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, confirmedBooking.getBookingStatus());
        assertNotNull(confirmedBooking.getCancellationTime());
        verify(reservationRepository, times(1)).releaseSeats(eq(Long.valueOf(1L)), eq(Arrays.asList(Long.valueOf(55L))));
        verify(reservationRepository, times(1)).deleteByBookingReference("CB-20261030-CONF");
        verify(bookingSeatRepository, times(1)).save(any(BookingSeat.class));
    }

    @Test
    @DisplayName("Cancel 2: Cancellation of another user's booking must fail")
    void testUnauthorizedCancellation() {
        when(bookingRepository.findById(Long.valueOf(10L))).thenReturn(confirmedBooking);
        when(authService.getCurrentAuthenticatedUser()).thenReturn(otherUser);

        try {
            bookingService.cancelBooking(Long.valueOf(10L));
            fail("Expected UnauthorizedException");
        } catch (UnauthorizedException expected) {
            assertNotNull(expected.getMessage());
        }
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancel 3: Already cancelled booking cannot be cancelled again")
    void testAlreadyCancelledBooking() {
        confirmedBooking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(Long.valueOf(10L))).thenReturn(confirmedBooking);
        when(authService.getCurrentAuthenticatedUser()).thenReturn(owner);

        try {
            bookingService.cancelBooking(Long.valueOf(10L));
            fail("Expected BadRequestException");
        } catch (BadRequestException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    @DisplayName("Cancel 4: Prevent cancellation after show time has passed")
    void testPreventCancellationAfterShowStarted() {
        futureShow.setShowDate(LocalDate.now().minusDays(1));
        when(bookingRepository.findById(Long.valueOf(10L))).thenReturn(confirmedBooking);
        when(authService.getCurrentAuthenticatedUser()).thenReturn(owner);

        try {
            bookingService.cancelBooking(Long.valueOf(10L));
            fail("Expected BadRequestException");
        } catch (BadRequestException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
