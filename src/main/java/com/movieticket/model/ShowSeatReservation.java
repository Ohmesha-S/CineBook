package com.movieticket.model;

import java.time.LocalDateTime;

/**
 * Unit 1: Java Fundamentals - Inheritance, Constructors, Encapsulation, Method Overriding.
 */
public class ShowSeatReservation extends BaseEntity {

    private Show show;
    private Seat seat;
    private User user;
    private String bookingReference;
    private String status = "HELD";
    private LocalDateTime expiresAt;

    public ShowSeatReservation() {
        super();
    }

    public ShowSeatReservation(Show show, Seat seat, User user, String bookingReference, String status, LocalDateTime expiresAt) {
        super();
        this.show = show;
        this.seat = seat;
        this.user = user;
        this.bookingReference = bookingReference;
        this.status = (status != null) ? status : "HELD";
        this.expiresAt = expiresAt;
    }

    @Override
    public String getEntitySummary() {
        return "ShowSeatReservation[id=" + getId() + ", ref=" + bookingReference + ", status=" + status + "]";
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }
}
