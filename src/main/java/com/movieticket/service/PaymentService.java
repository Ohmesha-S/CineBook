package com.movieticket.service;

import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.PaymentResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.PaymentMethod;
import com.movieticket.enums.PaymentStatus;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Booking;
import com.movieticket.model.Payment;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.PaymentRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import com.movieticket.service.payment.PaymentProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ShowSeatReservationRepository reservationRepository;
    private final Map<PaymentMethod, PaymentProcessor> paymentProcessors;

    public PaymentService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          ShowSeatReservationRepository reservationRepository,
                          List<PaymentProcessor> processors) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.paymentProcessors = processors.stream()
                .collect(Collectors.toMap(PaymentProcessor::getSupportedMethod, p -> p));
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findByBookingReference(request.getBookingReference())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + request.getBookingReference()));

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("This booking is already confirmed.");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot make payment on a cancelled booking.");
        }

        // Validate seat reservation has not expired
        List<ShowSeatReservation> reservations = reservationRepository.findByBookingReference(booking.getBookingReference());
        if (reservations.isEmpty()) {
            throw new BadRequestException("Seat reservation has expired or was released. Please book again.");
        }

        boolean anyExpired = reservations.stream().anyMatch(r -> LocalDateTime.now().isAfter(r.getExpiresAt()));
        if (anyExpired && !"SUCCESS".equalsIgnoreCase(booking.getBookingStatus().name())) {
            // Clean up and expire
            reservationRepository.deleteByBookingReference(booking.getBookingReference());
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BadRequestException("Seat reservation has expired. Please select your seats again.");
        }

        PaymentProcessor processor = paymentProcessors.get(request.getPaymentMethod());
        if (processor == null) {
            throw new BadRequestException("Unsupported payment method: " + request.getPaymentMethod());
        }

        PaymentResponse response = processor.processPayment(booking, request);

        // Persist payment attempt
        Payment payment = new Payment(
                booking,
                response.getPaymentReference(),
                response.getPaymentMethod(),
                response.getAmount(),
                response.getPaymentStatus()
        );
        payment = paymentRepository.save(payment);
        response.setId(payment.getId());

        if (response.getPaymentStatus() == PaymentStatus.SUCCESS) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // Confirm all held reservations
            for (ShowSeatReservation r : reservations) {
                r.setStatus("CONFIRMED");
            }
            reservationRepository.saveAll(reservations);
        } else {
            booking.setBookingStatus(BookingStatus.PAYMENT_FAILED);
            bookingRepository.save(booking);

            // Free temporary hold so seats are not stuck
            reservationRepository.deleteByBookingReference(booking.getBookingReference());
        }

        return response;
    }

    public PaymentResponse getPaymentByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found with reference: " + paymentReference));
        String bookingRef = payment.getBooking() != null ? payment.getBooking().getBookingReference() : "";
        return PaymentResponse.fromEntity(payment, bookingRef, "Payment details retrieved.");
    }
}
