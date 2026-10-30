package com.movieticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "show_seat_reservations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_show_seat_reservation", columnNames = {"show_id", "seat_id"})
    },
    indexes = {
        @Index(name = "idx_reservation_show", columnList = "show_id"),
        @Index(name = "idx_reservation_seat", columnList = "seat_id"),
        @Index(name = "idx_reservation_expiry", columnList = "expires_at")
    }
)
public class ShowSeatReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "booking_reference", length = 50)
    private String bookingReference;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "HELD"; // HELD, CONFIRMED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public ShowSeatReservation() {
    }

    public ShowSeatReservation(Show show, Seat seat, User user, String bookingReference, String status, LocalDateTime expiresAt) {
        this.show = show;
        this.seat = seat;
        this.user = user;
        this.bookingReference = bookingReference;
        this.status = status;
        this.expiresAt = expiresAt;
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
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
