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
public class NetBankingPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod getSupportedMethod() {
        return PaymentMethod.NET_BANKING;
    }

    @Override
    public PaymentResponse processPayment(Booking booking, PaymentRequest request) {
        boolean isSuccess = !"FAIL".equalsIgnoreCase(request.getSimulateOutcome());

        String ref = "NB-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(
                booking,
                ref,
                PaymentMethod.NET_BANKING,
                booking.getTotalAmount(),
                isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED
        );
        payment.setTransactionTime(LocalDateTime.now());

        String message = isSuccess
                ? "Net Banking simulated payment completed successfully."
                : "Net Banking payment simulation failed (Simulated bank server error).";

        return PaymentResponse.fromEntity(payment, booking.getBookingReference(), message);
    }
}
