package com.movieticket.service;

import com.movieticket.db.DatabaseManager;
import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.PaymentResponse;
import com.movieticket.enums.BookingStatus;
import com.movieticket.enums.PaymentStatus;
import com.movieticket.exception.BadRequestException;
import com.movieticket.exception.ResourceNotFoundException;
import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;
import com.movieticket.model.Payment;
import com.movieticket.model.ShowSeatReservation;
import com.movieticket.os.BookingMetricsCounter;
import com.movieticket.os.TicketNotificationService;
import com.movieticket.repository.BookingRepository;
import com.movieticket.repository.BookingSeatRepository;
import com.movieticket.repository.PaymentRepository;
import com.movieticket.repository.ShowSeatReservationRepository;
import com.movieticket.service.payment.PaymentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit 1, Unit 2, Unit 4 & Unit 6: Payment Service demonstrating Array of Objects (PaymentProcessor[]),
 * Dynamic Method Dispatch, HashMap lookup, manual JDBC ACID transactions, and Producer-Consumer notifications.
 */
@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ShowSeatReservationRepository reservationRepository;
    private final PaymentProcessor[] processorArray;
    private final Map<String, PaymentProcessor> paymentProcessors;
    private final DatabaseManager databaseManager;
    private final BookingMetricsCounter metricsCounter;
    private final TicketNotificationService notificationService;

    @Autowired
    public PaymentService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          PaymentRepository paymentRepository,
                          ShowSeatReservationRepository reservationRepository,
                          List<PaymentProcessor> processors,
                          DatabaseManager databaseManager,
                          BookingMetricsCounter metricsCounter,
                          TicketNotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.databaseManager = databaseManager;
        this.metricsCounter = metricsCounter;
        this.notificationService = notificationService;

        int size = (processors != null) ? processors.size() : 0;
        this.processorArray = new PaymentProcessor[size];
        this.paymentProcessors = new HashMap<String, PaymentProcessor>();

        for (int i = 0; i < size; i++) {
            PaymentProcessor processor = processors.get(i);
            this.processorArray[i] = processor;
            this.paymentProcessors.put(processor.getSupportedMethod(), processor);
        }
    }

    /**
     * Convenience constructor for standalone unit tests without Spring container.
     */
    public PaymentService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          ShowSeatReservationRepository reservationRepository,
                          List<PaymentProcessor> processors) {
        this(bookingRepository, null, paymentRepository, reservationRepository,
             processors, null, null, null);
    }

    public PaymentProcessor[] getProcessorArray() {
        return processorArray;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        if (request == null || request.getBookingReference() == null || request.getBookingReference().trim().isEmpty()) {
            throw new BadRequestException("Booking reference is required.");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new BadRequestException("Payment method is required.");
        }

        if (metricsCounter != null) {
            metricsCounter.incrementPaymentTransactions();
        }

        Connection con = null;
        try {
            if (databaseManager != null) {
                con = databaseManager.getConnection();
                if (con != null) {
                    con.setAutoCommit(false);
                }
            }

            String bookingRef = request.getBookingReference().trim();
            Booking booking = (con != null)
                    ? bookingRepository.findByBookingReference(con, bookingRef)
                    : bookingRepository.findByBookingReference(bookingRef);
            if (booking == null) {
                throw new ResourceNotFoundException("Booking not found with reference: " + bookingRef);
            }

            if (BookingStatus.CONFIRMED.equals(booking.getBookingStatus())) {
                throw new BadRequestException("This booking is already confirmed.");
            }
            if (BookingStatus.CANCELLED.equals(booking.getBookingStatus())) {
                throw new BadRequestException("Cannot make payment on a cancelled booking.");
            }

            List<ShowSeatReservation> reservations = (con != null)
                    ? reservationRepository.findByBookingReference(con, booking.getBookingReference())
                    : reservationRepository.findByBookingReference(booking.getBookingReference());
            if (reservations.isEmpty()) {
                throw new BadRequestException("Seat reservation has expired or was released. Please book again.");
            }

            boolean anyExpired = false;
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < reservations.size(); i++) {
                ShowSeatReservation r = reservations.get(i);
                if (r.getExpiresAt() != null && now.isAfter(r.getExpiresAt())) {
                    anyExpired = true;
                    break;
                }
            }

            if (anyExpired) {
                releaseBookingSeats(con, booking);
                if (con != null) {
                    reservationRepository.deleteByBookingReference(con, booking.getBookingReference());
                    booking.setBookingStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(con, booking);
                    con.commit();
                } else {
                    reservationRepository.deleteByBookingReference(booking.getBookingReference());
                    booking.setBookingStatus(BookingStatus.EXPIRED);
                    bookingRepository.save(booking);
                }
                throw new BadRequestException("Seat reservation has expired. Please select your seats again.");
            }

            PaymentProcessor processor = paymentProcessors.get(request.getPaymentMethod().trim().toUpperCase());
            if (processor == null) {
                throw new BadRequestException("Unsupported payment method: " + request.getPaymentMethod());
            }

            PaymentResponse response = processor.processPayment(booking, request);

            Payment payment = new Payment(
                    booking,
                    response.getPaymentReference(),
                    response.getPaymentMethod(),
                    response.getAmount(),
                    response.getPaymentStatus()
            );
            payment = (con != null) ? paymentRepository.save(con, payment) : paymentRepository.save(payment);
            response.setId(payment.getId());

            if (PaymentStatus.SUCCESS.equals(response.getPaymentStatus())) {
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                if (con != null) {
                    bookingRepository.save(con, booking);
                } else {
                    bookingRepository.save(booking);
                }

                for (int i = 0; i < reservations.size(); i++) {
                    ShowSeatReservation r = reservations.get(i);
                    r.setStatus("CONFIRMED");
                    if (con != null) {
                        reservationRepository.save(con, r);
                    } else {
                        reservationRepository.save(r);
                    }
                }

                if (con != null) {
                    con.commit();
                }

                if (metricsCounter != null) {
                    metricsCounter.incrementConfirmedBookings();
                }
                if (notificationService != null) {
                    notificationService.publishNotification("PAYMENT_CONFIRMED ref=" + booking.getBookingReference()
                            + " payRef=" + response.getPaymentReference() + " method=" + response.getPaymentMethod());
                }
            } else {
                booking.setBookingStatus(BookingStatus.PAYMENT_FAILED);
                releaseBookingSeats(con, booking);
                if (con != null) {
                    bookingRepository.save(con, booking);
                    reservationRepository.deleteByBookingReference(con, booking.getBookingReference());
                    con.commit();
                } else {
                    bookingRepository.save(booking);
                    reservationRepository.deleteByBookingReference(booking.getBookingReference());
                }

                if (notificationService != null) {
                    notificationService.publishNotification("PAYMENT_FAILED ref=" + booking.getBookingReference()
                            + " method=" + response.getPaymentMethod());
                }
            }

            return response;
        } catch (RuntimeException e) {
            if (databaseManager != null) {
                databaseManager.rollbackQuietly(con);
            }
            throw e;
        } catch (SQLException e) {
            if (databaseManager != null) {
                databaseManager.rollbackQuietly(con);
            }
            throw new RuntimeException("Database transaction failed during payment: " + e.getMessage(), e);
        } finally {
            DatabaseManager.closeQuietly(con);
        }
    }

    private void releaseBookingSeats(Connection con, Booking booking) {
        if (bookingSeatRepository == null || booking.getBookingSeats() == null) {
            return;
        }
        for (int i = 0; i < booking.getBookingSeats().size(); i++) {
            BookingSeat bs = booking.getBookingSeats().get(i);
            bs.setStatus("CANCELLED");
            if (con != null) {
                bookingSeatRepository.save(con, bs);
            } else {
                bookingSeatRepository.save(bs);
            }
        }
    }

    public PaymentResponse getPaymentByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment record not found with reference: " + paymentReference);
        }
        String bookingRef = payment.getBooking() != null ? payment.getBooking().getBookingReference() : "";
        return PaymentResponse.fromEntity(payment, bookingRef, "Payment details retrieved.");
    }
}
