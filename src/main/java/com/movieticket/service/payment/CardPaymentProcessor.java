package com.movieticket.service.payment;

import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.PaymentResponse;
import com.movieticket.enums.PaymentMethod;
import com.movieticket.enums.PaymentStatus;
import com.movieticket.model.Booking;
import com.movieticket.model.Payment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class CardPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod getSupportedMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public PaymentResponse processPayment(Booking booking, PaymentRequest request) {
        boolean isSuccess = !"FAIL".equalsIgnoreCase(request.getSimulateOutcome());

        String ref = "CARD-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(
                booking,
                ref,
                PaymentMethod.CARD,
                booking.getTotalAmount(),
                isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED
        );
        payment.setTransactionTime(LocalDateTime.now());

        String message = isSuccess
                ? "Credit/Debit Card payment simulation completed successfully."
                : "Card payment simulation failed (Simulated bank decline).";

        return PaymentResponse.fromEntity(payment, booking.getBookingReference(), message);
    }
}
