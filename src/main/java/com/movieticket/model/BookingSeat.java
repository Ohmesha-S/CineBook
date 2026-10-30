package com.movieticket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "booking_seats", indexes = {
    @Index(name = "idx_booking_seat_booking", columnList = "booking_id"),
    @Index(name = "idx_booking_seat_show", columnList = "show_id"),
    @Index(name = "idx_booking_seat_seat", columnList = "seat_id")
})
public class BookingSeat extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @NotNull(message = "Seat price is required")
    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "BOOKED"; // BOOKED, CANCELLED

    public BookingSeat() {
    }

    public BookingSeat(Booking booking, Seat seat, Show show, Double price, String status) {
        this.booking = booking;
        this.seat = seat;
        this.show = show;
        this.price = price;
        this.status = (status != null) ? status : "BOOKED";
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
