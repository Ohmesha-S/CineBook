package com.movieticket.dto;

import com.movieticket.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentRequest {

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod = PaymentMethod.CARD;

    // Simulation parameter: "SUCCESS" or "FAIL" (default: "SUCCESS")
    private String simulateOutcome = "SUCCESS";

    public PaymentRequest() {
    }

    public PaymentRequest(String bookingReference, PaymentMethod paymentMethod, String simulateOutcome) {
        this.bookingReference = bookingReference;
        this.paymentMethod = paymentMethod;
        this.simulateOutcome = simulateOutcome != null ? simulateOutcome : "SUCCESS";
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSimulateOutcome() {
        return simulateOutcome;
    }

    public void setSimulateOutcome(String simulateOutcome) {
        this.simulateOutcome = simulateOutcome;
    }
}
