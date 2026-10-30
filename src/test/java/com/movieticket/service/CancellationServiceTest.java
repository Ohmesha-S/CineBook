package com.movieticket.service;

import com.movieticket.dto.BookingResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.UnauthorizedException;
import com.movieticket.model.*;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.BookingSeatRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CancellationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private ShowSeatReservationRepository reservationRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private BookingService bookingService;

    private User owner;
    private User otherUser;
    private Show futureShow;
    private Booking confirmedBooking;
    private Seat seat1;

    @BeforeEach
    void setUp() {
        owner = new User("Rahul", "rahul@cinebook.com", "123", "pass", UserRole.CUSTOMER);
        owner.setId(1L);

        otherUser = new User("Other", "other@cinebook.com", "456", "pass", UserRole.CUSTOMER);
        otherUser.setId(2L);

        futureShow = new Show(null, null, LocalDate.now().plusDays(2), LocalTime.of(18, 0), LocalTime.of(21, 0), ShowStatus.SCHEDULED);
        futureShow.setId(1L);

        confirmedBooking = new Booking("CB-20261030-CONF", owner, futureShow, 200.0, BookingStatus.CONFIRMED);
        confirmedBooking.setId(10L);

        seat1 = new Seat(null, "B", 4, null, 180.0);
        seat1.setId(55L);
        BookingSeat bs = new BookingSeat(confirmedBooking, seat1, futureShow, 180.0, "BOOKED");
        List<BookingSeat> seats = new ArrayList<>();
        seats.add(bs);
        confirmedBooking.setBookingSeats(seats);
    }

    @Test
    @DisplayName("Cancel 1: Successful cancellation releases seats")
    void testSuccessfulCancellation() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(authService.getCurrentAuthenticatedUser()).thenReturn(owner);
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);

        BookingResponse response = bookingService.cancelBooking(10L);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, confirmedBooking.getBookingStatus());
        assertNotNull(confirmedBooking.getCancellationTime());
        // Verify seats release in reservation and booking_seats
        verify(reservationRepository, times(1)).releaseSeats(eq(1L), eq(List.of(55L)));
        verify(reservationRepository, times(1)).deleteByBookingReference("CB-20261030-CONF");
        verify(bookingSeatRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Cancel 2: Cancellation of another user's booking must fail")
    void testUnauthorizedCancellation() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(authService.getCurrentAuthenticatedUser()).thenReturn(otherUser); // not the owner

        assertThrows(UnauthorizedException.class, () -> bookingService.cancelBooking(10L));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel 3: Already cancelled booking cannot be cancelled again")
    void testAlreadyCancelledBooking() {
        confirmedBooking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(authService.getCurrentAuthenticatedUser()).thenReturn(owner);

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(10L));
    }

    @Test
    @DisplayName("Cancel 4: Prevent cancellation after show time has passed")
    void testPreventCancellationAfterShowStarted() {
        futureShow.setShowDate(LocalDate.now().minusDays(1)); // Past show
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(authService.getCurrentAuthenticatedUser()).thenReturn(owner);

        assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(10L));
    }
}
