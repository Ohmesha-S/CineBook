package com.movieticket.model;

/**
 * Unit 1: Java Fundamentals - Inheritance, Constructors, Encapsulation, Method Overriding.
 */
public class BookingSeat extends BaseEntity {

    private Booking booking;
    private Seat seat;
    private Show show;
    private Double price;
    private String status = "BOOKED";

    public BookingSeat() {
        super();
    }

    public BookingSeat(Booking booking, Seat seat, Show show, Double price, String status) {
        super();
        this.booking = booking;
        this.seat = seat;
        this.show = show;
        this.price = price;
        this.status = (status != null) ? status : "BOOKED";
    }

    @Override
    public String getEntitySummary() {
        String seatLabel = (seat != null) ? seat.getSeatIdentifier() : "N/A";
        return "BookingSeat[id=" + getId() + ", seat=" + seatLabel + ", status=" + status + "]";
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
