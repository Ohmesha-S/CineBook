package com.movieticket.service.payment;

import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.PaymentResponse;
import com.movieticket.enums.PaymentStatus;
import com.movieticket.model.Booking;
import com.movieticket.model.Payment;
import com.movieticket.util.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Unit 1: Java Fundamentals - Abstract Class implementing an Interface,
 * Inheritance,
 * Method Overriding, and Dynamic Method Dispatch across payment processors.
 */
public abstract class AbstractPaymentProcessor implements PaymentProcessor {

    protected abstract String getReferencePrefix();

    protected abstract String getSuccessMessage();

    protected abstract String getFailureMessage();

    @Override
    public PaymentResponse processPayment(Booking booking, PaymentRequest request) {
        String outcome = (request != null) ? request.getSimulateOutcome() : "SUCCESS";
        boolean isSuccess = !"FAIL".equalsIgnoreCase(outcome);

        String ref = getReferencePrefix() + "-" + PasswordEncoder.generateToken(8);
        String status = isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        Payment payment = new Payment(
                booking,
                ref,
                getSupportedMethod(),
                booking.getTotalAmount(),
                status);
        payment.setTransactionTime(LocalDateTime.now());

        String message = isSuccess ? getSuccessMessage() : getFailureMessage();
        return PaymentResponse.fromEntity(payment, booking.getBookingReference(), message);
    }
}
