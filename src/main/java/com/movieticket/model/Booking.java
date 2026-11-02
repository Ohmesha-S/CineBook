package com.movieticket.model;

import com.movieticket.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 1: Java Fundamentals - Inheritance, Constructors, Encapsulation, Method Overriding.
 */
public class Booking extends BaseEntity {

    private String bookingReference;
    private User user;
    private Show show;
    private Double totalAmount;
    private String bookingStatus = BookingStatus.PENDING_PAYMENT;
    private LocalDateTime bookingTime;
    private LocalDateTime cancellationTime;
    private List<BookingSeat> bookingSeats = new ArrayList<BookingSeat>();
    private List<Payment> payments = new ArrayList<Payment>();

    public Booking() {
        super();
    }

    public Booking(String bookingReference, User user, Show show, Double totalAmount, String bookingStatus) {
        super();
        this.bookingReference = bookingReference;
        this.user = user;
        this.show = show;
        this.totalAmount = totalAmount;
        this.bookingStatus = (bookingStatus != null) ? bookingStatus : BookingStatus.PENDING_PAYMENT;
        this.bookingTime = LocalDateTime.now();
    }

    @Override
    public String getEntitySummary() {
        return "Booking[id=" + getId() + ", ref=" + bookingReference + ", status=" + bookingStatus + "]";
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public LocalDateTime getCancellationTime() {
        return cancellationTime;
    }

    public void setCancellationTime(LocalDateTime cancellationTime) {
        this.cancellationTime = cancellationTime;
    }

    public List<BookingSeat> getBookingSeats() {
        return bookingSeats;
    }

    public void setBookingSeats(List<BookingSeat> bookingSeats) {
        this.bookingSeats = bookingSeats;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }
}
