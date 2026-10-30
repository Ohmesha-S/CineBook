package com.movieticket.service;

import com.movieticket.dto.BookingRequest;
import com.movieticket.dto.BookingResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.BookingConflictException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.exception.UnauthorizedException;
import com.movieticket.model.*;
import com.movieticket.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowSeatReservationRepository reservationRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final AuthService authService;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          ShowSeatReservationRepository reservationRepository,
                          ShowRepository showRepository,
                          SeatRepository seatRepository,
                          AuthService authService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.reservationRepository = reservationRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.authService = authService;
    }

    /**
     * Creates a new booking with temporary seat reservations.
     * Enforces database transactions and concurrency locking so two users cannot reserve the same seat.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(BookingRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new BadRequestException("Please select at least one seat.");
        }

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + request.getShowId()));

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new BadRequestException("This show has been cancelled.");
        }

        // Validate show is in future
        LocalDateTime showStart = LocalDateTime.of(show.getShowDate(), show.getStartTime());
        if (LocalDateTime.now().isAfter(showStart)) {
            throw new BadRequestException("Cannot book tickets for past shows.");
        }

        // Cleanup expired holds
        reservationRepository.cleanupExpiredHolds(LocalDateTime.now());

        // Validate selected seats
        List<Seat> seats = seatRepository.findByIdIn(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new BadRequestException("One or more selected seat IDs are invalid.");
        }

        // Check each seat belongs to show's screen
        for (Seat s : seats) {
            if (!s.getScreen().getId().equals(show.getScreen().getId())) {
                throw new BadRequestException("Seat " + s.getSeatIdentifier() + " does not belong to this screen.");
            }
        }

        // Check for concurrent booking conflicts:
        // 1. Check if already confirmed in booking_seats
        for (Seat s : seats) {
            boolean alreadyBooked = bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(show.getId(), s.getId(), "BOOKED");
            if (alreadyBooked) {
                throw new BookingConflictException("Seat " + s.getSeatIdentifier() + " is already booked.");
            }
        }

        // 2. Check active reservations in show_seat_reservations
        List<ShowSeatReservation> activeReservations = reservationRepository.findActiveReservationsByShow(show.getId(), LocalDateTime.now());
        Set<Long> reservedSeatIds = activeReservations.stream()
                .map(r -> r.getSeat().getId())
                .collect(Collectors.toSet());

        for (Seat s : seats) {
            if (reservedSeatIds.contains(s.getId())) {
                throw new BookingConflictException("Seat " + s.getSeatIdentifier() + " is currently on hold or booked by another customer.");
            }
        }

        // Calculate amount strictly on backend
        double multiplier = show.getPriceMultiplier() != null ? show.getPriceMultiplier() : 1.0;
        double subtotal = 0.0;
        for (Seat s : seats) {
            double price = Math.round(s.getBasePrice() * multiplier * 100.0) / 100.0;
            subtotal += price;
        }

        double convenienceFee = 20.00; // Flat convenience fee
        double totalAmount = Math.round((subtotal + convenienceFee) * 100.0) / 100.0;

        // Generate unique booking reference
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String bookingReference = "CB-" + datePrefix + "-" + randomSuffix;

        Booking booking = new Booking(bookingReference, currentUser, show, totalAmount, BookingStatus.PENDING_PAYMENT);
        booking = bookingRepository.save(booking);

        // Save BookingSeat entities and lock seats in show_seat_reservations
        LocalDateTime holdExpiry = LocalDateTime.now().plusMinutes(10); // 10 minute hold for payment
        List<BookingSeat> bookingSeats = new ArrayList<>();
        List<ShowSeatReservation> reservations = new ArrayList<>();

        try {
            for (Seat s : seats) {
                double seatPrice = Math.round(s.getBasePrice() * multiplier * 100.0) / 100.0;
                BookingSeat bs = new BookingSeat(booking, s, show, seatPrice, "BOOKED");
                bookingSeats.add(bs);

                ShowSeatReservation reservation = new ShowSeatReservation(
                        show, s, currentUser, bookingReference, "HELD", holdExpiry
                );
                reservations.add(reservation);
            }

            bookingSeatRepository.saveAll(bookingSeats);
            reservationRepository.saveAll(reservations);
            booking.setBookingSeats(bookingSeats);
        } catch (DataIntegrityViolationException ex) {
            throw new BookingConflictException("One or more selected seats were just reserved by another customer. Please select different seats.");
        }

        return BookingResponse.fromEntity(booking);
    }

    public BookingResponse getBookingByReference(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + bookingReference));

        User currentUser = authService.getCurrentAuthenticatedUser();
        // Only owner or admin can view
        if (currentUser.getRole() != UserRole.ADMIN && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view this booking.");
        }

        return BookingResponse.fromEntity(booking);
    }

    public Booking getBookingEntityByReference(String reference) {
        return bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + reference));
    }

    public List<BookingResponse> getMyBookings() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> adminGetAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Cancels an eligible booking and releases all booked seats back into availability.
     */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        User currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.getRole() != UserRole.ADMIN && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to cancel this booking.");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("This booking is already cancelled.");
        }

        // Validate cancellation eligibility based on show start time
        Show show = booking.getShow();
        if (show != null) {
            LocalDateTime showDateTime = LocalDateTime.of(show.getShowDate(), show.getStartTime());
            if (LocalDateTime.now().isAfter(showDateTime)) {
                throw new BadRequestException("This booking is no longer eligible for cancellation as the show time has passed.");
            }
        }

        // Update booking status
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancellationTime(LocalDateTime.now());

        // Update booking_seats status
        List<Long> seatIds = new ArrayList<>();
        if (booking.getBookingSeats() != null) {
            for (BookingSeat bs : booking.getBookingSeats()) {
                bs.setStatus("CANCELLED");
                if (bs.getSeat() != null) {
                    seatIds.add(bs.getSeat().getId());
                }
            }
            bookingSeatRepository.saveAll(booking.getBookingSeats());
        }

        // Release show_seat_reservations so seats become available immediately
        if (show != null && !seatIds.isEmpty()) {
            reservationRepository.releaseSeats(show.getId(), seatIds);
        }
        reservationRepository.deleteByBookingReference(booking.getBookingReference());

        booking = bookingRepository.save(booking);
        return BookingResponse.fromEntity(booking);
    }
}
