package com.movieticket.service;

import com.movieticket.dto.TicketResponse;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.exception.UnauthorizedException;
import com.movieticket.model.Booking;
import com.movieticket.model.Payment;
import com.movieticket.model.User;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final AuthService authService;

    public TicketService(BookingRepository bookingRepository,
                         PaymentRepository paymentRepository,
                         AuthService authService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.authService = authService;
    }

    public TicketResponse getTicket(String bookingReference) {
        return getTicketForUser(bookingReference, authService.getCurrentAuthenticatedUser());
    }

    public TicketResponse getTicket(String bookingReference, String authHeader) {
        return getTicketForUser(bookingReference, authService.resolveUserFromHeader(authHeader));
    }

    private TicketResponse getTicketForUser(String bookingReference, User currentUser) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference);
        if (booking == null) {
            throw new ResourceNotFoundException("Booking not found with reference: " + bookingReference);
        }

        if (!UserRole.ADMIN.equals(currentUser.getRole()) && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view this ticket.");
        }

        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        String paymentStatus = payments.isEmpty() ? "PENDING" : payments.get(0).getPaymentStatus();

        return TicketResponse.fromBooking(booking, paymentStatus);
    }
}
