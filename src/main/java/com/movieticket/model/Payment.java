package com.movieticket.model;

import com.movieticket.enums.PaymentMethod;
import com.movieticket.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * Unit 1: Java Fundamentals - Inheritance, Constructors, Encapsulation, Method Overriding.
 */
public class Payment extends BaseEntity {

    private Booking booking;
    private String paymentReference;
    private String paymentMethod = PaymentMethod.CARD;
    private Double amount;
    private String paymentStatus = PaymentStatus.INITIATED;
    private LocalDateTime transactionTime;

    public Payment() {
        super();
    }

    public Payment(Booking booking, String paymentReference, String paymentMethod, Double amount, String paymentStatus) {
        super();
        this.booking = booking;
        this.paymentReference = paymentReference;
        this.paymentMethod = (paymentMethod != null) ? paymentMethod : PaymentMethod.CARD;
        this.amount = amount;
        this.paymentStatus = (paymentStatus != null) ? paymentStatus : PaymentStatus.INITIATED;
        this.transactionTime = LocalDateTime.now();
    }

    @Override
    public String getEntitySummary() {
        return "Payment[id=" + getId() + ", ref=" + paymentReference + ", status=" + paymentStatus + "]";
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }
}
