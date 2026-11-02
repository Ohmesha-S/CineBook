package com.movieticket.service;

import com.movieticket.db.DatabaseManager;
import com.movieticket.dto.BookingRequest;
import com.movieticket.dto.BookingResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.ShowStatus;
import com.movieticket.enums.UserRole;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.BookingConflictException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.exception.UnauthorizedException;
import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Seat;
import com.movieticket.model.Show;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.model.User;
import com.movieticket.model.comparator.BookingTimeDescComparator;
import com.movieticket.os.BookingMetricsCounter;
import com.movieticket.os.SeatLockManager;
import com.movieticket.os.SeatLockTask;
import com.movieticket.os.TicketNotificationService;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.BookingSeatRepository;
import com.movieticket.repository.SeatRepository;
import com.movieticket.repository.ShowRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import com.movieticket.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.movieticket.util.PasswordEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit 4 & Unit 6: Booking Service combining manual JDBC ACID transactions (setAutoCommit(false),
 * commit(), rollback()) with Unit 6 Deadlock-Free Ordered Seat Locking (SeatLockManager),
 * Synchronized Counters (BookingMetricsCounter), and Producer-Consumer Notifications.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowSeatReservationRepository reservationRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final AuthService authService;
    private final DatabaseManager databaseManager;
    private final SeatLockManager seatLockManager;
    private final BookingMetricsCounter metricsCounter;
    private final TicketNotificationService notificationService;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          ShowSeatReservationRepository reservationRepository,
                          ShowRepository showRepository,
                          SeatRepository seatRepository,
                          AuthService authService,
                          DatabaseManager databaseManager,
                          SeatLockManager seatLockManager,
                          BookingMetricsCounter metricsCounter,
                          TicketNotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.reservationRepository = reservationRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.authService = authService;
        this.databaseManager = databaseManager;
        this.seatLockManager = seatLockManager;
        this.metricsCounter = metricsCounter;
        this.notificationService = notificationService;
    }

    /**
     * Convenience constructor for standalone unit tests without Spring container.
     */
    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          ShowSeatReservationRepository reservationRepository,
                          ShowRepository showRepository,
                          SeatRepository seatRepository,
                          AuthService authService) {
        this(bookingRepository, bookingSeatRepository, reservationRepository,
             showRepository, seatRepository, authService,
             null, new SeatLockManager(null), null, null);
    }

    public BookingResponse createBooking(BookingRequest request) {
        return createBookingForUser(request, authService.getCurrentAuthenticatedUser());
    }

    public BookingResponse createBooking(BookingRequest request, String authHeader) {
        return createBookingForUser(request, authService.resolveUserFromHeader(authHeader));
    }

    private BookingResponse createBookingForUser(final BookingRequest request, final User currentUser) {
        if (metricsCounter != null) {
            metricsCounter.incrementBookingAttempts();
        }

        if (request == null || request.getShowId() == null) {
            throw new BadRequestException("Show ID is required.");
        }
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new BadRequestException("Please select at least one seat.");
        }

        Set<Long> uniqueIdSet = new HashSet<Long>();
        for (int i = 0; i < request.getSeatIds().size(); i++) {
            Long sid = request.getSeatIds().get(i);
            if (sid != null) {
                uniqueIdSet.add(sid);
            }
        }
        final List<Long> requestedSeatIds = new ArrayList<Long>(uniqueIdSet);
        if (requestedSeatIds.isEmpty()) {
            throw new BadRequestException("Please select at least one valid seat.");
        }

        SeatLockTask<BookingResponse> criticalSection = new SeatLockTask<BookingResponse>() {
            @Override
            public BookingResponse execute() {
                return executeCreateBookingTransaction(request.getShowId(), requestedSeatIds, currentUser);
            }
        };

        if (seatLockManager != null) {
            return seatLockManager.executeWithOrderedSeatLocks(request.getShowId(), requestedSeatIds, criticalSection);
        }
        return criticalSection.execute();
    }

    private BookingResponse executeCreateBookingTransaction(Long showId, List<Long> seatIds, User currentUser) {
        Connection con = null;
        try {
            if (databaseManager != null) {
                con = databaseManager.getConnection();
                if (con != null) {
                    con.setAutoCommit(false);
                }
            }

            Show show = (con != null) ? showRepository.findById(con, showId) : showRepository.findById(showId);
            if (show == null) {
                throw new ResourceNotFoundException("Show not found with id: " + showId);
            }

            if (ShowStatus.CANCELLED.equals(show.getStatus())) {
                throw new BadRequestException("This show has been cancelled.");
            }

            LocalDateTime showStart = LocalDateTime.of(show.getShowDate(), show.getStartTime());
            if (LocalDateTime.now().isAfter(showStart)) {
                throw new BadRequestException("Cannot book tickets for past shows.");
            }

            LocalDateTime now = LocalDateTime.now();
            if (con != null) {
                reservationRepository.cleanupExpiredHolds(con, now);
            } else {
                reservationRepository.cleanupExpiredHolds(now);
            }

            List<Seat> seats = (con != null) ? seatRepository.findByIdIn(con, seatIds) : seatRepository.findByIdIn(seatIds);
            if (seats.size() != seatIds.size()) {
                throw new BadRequestException("One or more selected seat IDs are invalid.");
            }
            CollectionUtils.sortNaturally(seats);

            for (int i = 0; i < seats.size(); i++) {
                Seat s = seats.get(i);
                if (!s.getScreen().getId().equals(show.getScreen().getId())) {
                    throw new BadRequestException("Seat " + s.getSeatIdentifier() + " does not belong to this screen.");
                }
            }

            for (int i = 0; i < seats.size(); i++) {
                Seat s = seats.get(i);
                boolean alreadyBooked = (con != null)
                        ? bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(con, show.getId(), s.getId(), "BOOKED")
                        : bookingSeatRepository.existsByShowIdAndSeatIdAndStatus(show.getId(), s.getId(), "BOOKED");
                if (alreadyBooked) {
                    throw new BookingConflictException("Seat " + s.getSeatIdentifier() + " is already booked.");
                }
            }

            List<ShowSeatReservation> activeReservations = reservationRepository.findActiveReservationsByShow(show.getId(), now);
            Set<Long> reservedSeatIds = new HashSet<Long>();
            for (int i = 0; i < activeReservations.size(); i++) {
                ShowSeatReservation r = activeReservations.get(i);
                if (r.getSeat() != null && r.getSeat().getId() != null) {
                    reservedSeatIds.add(r.getSeat().getId());
                }
            }

            for (int i = 0; i < seats.size(); i++) {
                Seat s = seats.get(i);
                if (reservedSeatIds.contains(s.getId())) {
                    throw new BookingConflictException("Seat " + s.getSeatIdentifier()
                            + " is currently on hold or booked by another customer.");
                }
            }

            double multiplier = show.getPriceMultiplier() != null ? show.getPriceMultiplier() : 1.0;
            double subtotal = 0.0;
            for (int i = 0; i < seats.size(); i++) {
                Seat s = seats.get(i);
                double price = Math.round(s.getBasePrice() * multiplier * 100.0) / 100.0;
                subtotal += price;
            }

            double convenienceFee = 20.00;
            double totalAmount = Math.round((subtotal + convenienceFee) * 100.0) / 100.0;

            LocalDate today = LocalDate.now();
            String datePrefix = String.format("%04d%02d%02d",
                    Integer.valueOf(today.getYear()),
                    Integer.valueOf(today.getMonthValue()),
                    Integer.valueOf(today.getDayOfMonth()));
            String randomSuffix = PasswordEncoder.generateToken(6);
            String bookingReference = "CB-" + datePrefix + "-" + randomSuffix;

            Booking booking = new Booking(
                    bookingReference, currentUser, show, Double.valueOf(totalAmount), BookingStatus.PENDING_PAYMENT);
            booking = (con != null) ? bookingRepository.save(con, booking) : bookingRepository.save(booking);

            LocalDateTime holdExpiry = LocalDateTime.now().plusMinutes(10);
            List<BookingSeat> bookingSeats = new ArrayList<BookingSeat>();

            for (int i = 0; i < seats.size(); i++) {
                Seat s = seats.get(i);
                double seatPrice = Math.round(s.getBasePrice() * multiplier * 100.0) / 100.0;
                BookingSeat bs = new BookingSeat(booking, s, show, Double.valueOf(seatPrice), "BOOKED");
                bs = (con != null) ? bookingSeatRepository.save(con, bs) : bookingSeatRepository.save(bs);
                bookingSeats.add(bs);

                ShowSeatReservation reservation = new ShowSeatReservation(
                        show, s, currentUser, bookingReference, "HELD", holdExpiry
                );
                if (con != null) {
                    reservationRepository.save(con, reservation);
                } else {
                    reservationRepository.save(reservation);
                }
            }

            booking.setBookingSeats(bookingSeats);

            if (con != null) {
                con.commit();
            }

            if (notificationService != null) {
                notificationService.publishNotification("BOOKING_HELD ref=" + bookingReference
                        + " user=" + currentUser.getEmail() + " seats=" + seats.size() + " total=INR " + totalAmount);
            }

            return BookingResponse.fromEntity(booking);
        } catch (RuntimeException e) {
            if (databaseManager != null) {
                databaseManager.rollbackQuietly(con);
            }
            throw e;
        } catch (SQLException e) {
            if (databaseManager != null) {
                databaseManager.rollbackQuietly(con);
            }
            throw new RuntimeException("Database transaction failed during booking: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    public BookingResponse getBookingByReference(String bookingReference) {
        return getBookingByReferenceForUser(bookingReference, authService.getCurrentAuthenticatedUser());
    }

    public BookingResponse getBookingByReference(String bookingReference, String authHeader) {
        return getBookingByReferenceForUser(bookingReference, authService.resolveUserFromHeader(authHeader));
    }

    private BookingResponse getBookingByReferenceForUser(String bookingReference, User currentUser) {
        Booking booking = getBookingEntityByReference(bookingReference);
        if (!UserRole.ADMIN.equals(currentUser.getRole()) && !booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view this booking.");
        }
        return BookingResponse.fromEntity(booking);
    }

    public Booking getBookingEntityByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference);
        if (booking == null) {
            throw new ResourceNotFoundException("Booking not found with reference: " + reference);
        }
        return booking;
    }

    public List<BookingResponse> getMyBookings() {
        return getMyBookingsForUser(authService.getCurrentAuthenticatedUser());
    }

    public List<BookingResponse> getMyBookings(String authHeader) {
        return getMyBookingsForUser(authService.resolveUserFromHeader(authHeader));
    }

    private List<BookingResponse> getMyBookingsForUser(User currentUser) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        CollectionUtils.sortWithComparator(bookings, new BookingTimeDescComparator());
        return toResponseList(bookings);
    }

    public List<BookingResponse> adminGetAllBookings() {
        List<Booking> bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        CollectionUtils.sortWithComparator(bookings, new BookingTimeDescComparator());
        return toResponseList(bookings);
    }

    public BookingResponse cancelBooking(Long bookingId) {
        return cancelBookingForUser(bookingId, authService.getCurrentAuthenticatedUser());
    }

    public BookingResponse cancelBooking(Long bookingId, String authHeader) {
        return cancelBookingForUser(bookingId, authService.resolveUserFromHeader(authHeader));
    }

    private BookingResponse cancelBookingForUser(Long bookingId, User currentUser) {
        Connection con = null;
        try {
            if (databaseManager != null) {
                con = databaseManager.getConnection();
                if (con != null) {
                    con.setAutoCommit(false);
                }
            }

            Booking booking = (con != null) ? bookingRepository.findById(con, bookingId) : bookingRepository.findById(bookingId);
            if (booking == null) {
                throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
            }

            if (!UserRole.ADMIN.equals(currentUser.getRole()) && !booking.getUser().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("You are not authorized to cancel this booking.");
            }

            if (BookingStatus.CANCELLED.equals(booking.getBookingStatus())) {
                throw new BadRequestException("This booking is already cancelled.");
            }

            Show show = booking.getShow();
            if (show != null && show.getShowDate() != null && show.getStartTime() != null) {
                LocalDateTime showDateTime = LocalDateTime.of(show.getShowDate(), show.getStartTime());
                if (LocalDateTime.now().isAfter(showDateTime)) {
                    throw new BadRequestException(
                            "This booking is no longer eligible for cancellation as the show time has passed.");
                }
            }

            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setCancellationTime(LocalDateTime.now());

            List<Long> seatIds = new ArrayList<Long>();
            if (booking.getBookingSeats() != null) {
                for (int i = 0; i < booking.getBookingSeats().size(); i++) {
                    BookingSeat bs = booking.getBookingSeats().get(i);
                    bs.setStatus("CANCELLED");
                    if (con != null) {
                        bookingSeatRepository.save(con, bs);
                    } else {
                        bookingSeatRepository.save(bs);
                    }
                    if (bs.getSeat() != null && bs.getSeat().getId() != null) {
                        seatIds.add(bs.getSeat().getId());
                    }
                }
            }

            if (show != null && !seatIds.isEmpty()) {
                if (con != null) {
                    reservationRepository.releaseSeats(con, show.getId(), seatIds);
                } else {
                    reservationRepository.releaseSeats(show.getId(), seatIds);
                }
            }
            if (con != null) {
                reservationRepository.deleteByBookingReference(con, booking.getBookingReference());
                booking = bookingRepository.save(con, booking);
                con.commit();
            } else {
                reservationRepository.deleteByBookingReference(booking.getBookingReference());
                booking = bookingRepository.save(booking);
            }

            if (metricsCounter != null) {
                metricsCounter.incrementCancelledBookings();
            }
            if (notificationService != null) {
                notificationService.publishNotification("BOOKING_CANCELLED ref=" + booking.getBookingReference()
                        + " by=" + currentUser.getEmail());
            }

            return BookingResponse.fromEntity(booking);
        } catch (RuntimeException e) {
            if (databaseManager != null) {
                databaseManager.rollbackQuietly(con);
            }
            throw e;
        } catch (SQLException e) {
            if (databaseManager != null) {
                databaseManager.rollbackQuietly(con);
            }
            throw new RuntimeException("Database transaction failed during cancellation: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    private List<BookingResponse> toResponseList(List<Booking> bookings) {
        List<BookingResponse> list = new ArrayList<BookingResponse>();
        if (bookings == null) {
            return list;
        }
        for (int i = 0; i < bookings.size(); i++) {
            list.add(BookingResponse.fromEntity(bookings.get(i)));
        }
        return list;
    }
}
