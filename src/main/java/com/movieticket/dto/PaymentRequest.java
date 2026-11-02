package com.movieticket.dto;

import com.movieticket.enums.PaymentMethod;

public class PaymentRequest {

    private String bookingReference;
    private String paymentMethod = PaymentMethod.CARD;
    private String simulateOutcome = "SUCCESS";

    public PaymentRequest() {
    }

    public PaymentRequest(String bookingReference, String paymentMethod, String simulateOutcome) {
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSimulateOutcome() {
        return simulateOutcome;
    }

    public void setSimulateOutcome(String simulateOutcome) {
        this.simulateOutcome = simulateOutcome;
    }
}
