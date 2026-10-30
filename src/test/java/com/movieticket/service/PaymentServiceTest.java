package com.movieticket.service;

import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.PaymentResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.PaymentMethod;
import com.movieticket.enums.PaymentStatus;
import com.movieticket.exception.BadRequestException;
import com.movieticket.model.Booking;
import com.movieticket.model.Payment;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.PaymentRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import com.movieticket.service.payment.CardPaymentProcessor;
import com.movieticket.service.payment.PaymentProcessor;
import com.movieticket.service.payment.UpiPaymentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ShowSeatReservationRepository reservationRepository;

    private PaymentService paymentService;

    private Booking pendingBooking;
    private ShowSeatReservation activeReservation;

    @BeforeEach
    void setUp() {
        List<PaymentProcessor> processors = Arrays.asList(
                new CardPaymentProcessor(),
                new UpiPaymentProcessor()
        );
        paymentService = new PaymentService(bookingRepository, paymentRepository, reservationRepository, processors);

        pendingBooking = new Booking("CB-20261030-XYZ", null, null, 320.0, BookingStatus.PENDING_PAYMENT);
        pendingBooking.setId(1L);

        activeReservation = new ShowSeatReservation(null, null, null, "CB-20261030-XYZ", "HELD", LocalDateTime.now().plusMinutes(10));
    }

    @Test
    @DisplayName("Payment 1: Successful simulated payment confirms booking and sets reservation to CONFIRMED")
    void testSuccessfulPayment() {
        PaymentRequest req = new PaymentRequest("CB-20261030-XYZ", PaymentMethod.CARD, "SUCCESS");

        when(bookingRepository.findByBookingReference("CB-20261030-XYZ")).thenReturn(Optional.of(pendingBooking));
        when(reservationRepository.findByBookingReference("CB-20261030-XYZ")).thenReturn(List.of(activeReservation));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(50L);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(req);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertEquals(BookingStatus.CONFIRMED, pendingBooking.getBookingStatus());
        assertEquals("CONFIRMED", activeReservation.getStatus());
        verify(bookingRepository, times(1)).save(pendingBooking);
        verify(reservationRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Payment 2: Failed simulated payment marks booking PAYMENT_FAILED and releases seats")
    void testFailedPaymentReleasesSeats() {
        PaymentRequest req = new PaymentRequest("CB-20261030-XYZ", PaymentMethod.UPI, "FAIL");

        when(bookingRepository.findByBookingReference("CB-20261030-XYZ")).thenReturn(Optional.of(pendingBooking));
        when(reservationRepository.findByBookingReference("CB-20261030-XYZ")).thenReturn(List.of(activeReservation));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(51L);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(req);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
        assertEquals(BookingStatus.PAYMENT_FAILED, pendingBooking.getBookingStatus());
        // Temporary hold should be released so seat is not stuck
        verify(reservationRepository, times(1)).deleteByBookingReference("CB-20261030-XYZ");
        verify(bookingRepository, times(1)).save(pendingBooking);
    }

    @Test
    @DisplayName("Payment 3: Expired seat reservation rejection")
    void testExpiredReservationRejection() {
        activeReservation.setExpiresAt(LocalDateTime.now().minusMinutes(2)); // Expired
        PaymentRequest req = new PaymentRequest("CB-20261030-XYZ", PaymentMethod.CARD, "SUCCESS");

        when(bookingRepository.findByBookingReference("CB-20261030-XYZ")).thenReturn(Optional.of(pendingBooking));
        when(reservationRepository.findByBookingReference("CB-20261030-XYZ")).thenReturn(List.of(activeReservation));

        assertThrows(BadRequestException.class, () -> paymentService.processPayment(req));
        verify(paymentRepository, never()).save(any());
    }
}
