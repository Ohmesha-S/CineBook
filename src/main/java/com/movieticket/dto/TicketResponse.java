package com.movieticket.dto;

import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TicketResponse {
    private String bookingReference;
    private String ticketNumber;
    private String customerName;
    private String customerEmail;
    private String movieTitle;
    private String moviePoster;
    private String movieGenre;
    private String movieLanguage;
    private Integer durationMinutes;
    private String theatreName;
    private String theatreLocation;
    private String theatreAddress;
    private String screenName;
    private String screenType;
    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<String> seatNumbers = new ArrayList<String>();
    private Double totalAmount;
    private String bookingStatus;
    private String paymentStatus;
    private LocalDateTime bookingTime;
    private String verificationCode;

    public TicketResponse() {
    }

    public static TicketResponse fromBooking(Booking booking, String paymentStatus) {
        if (booking == null) {
            return null;
        }
        TicketResponse ticket = new TicketResponse();
        ticket.setBookingReference(booking.getBookingReference());
        ticket.setTicketNumber("TKT-" + booking.getBookingReference());
        ticket.setVerificationCode("VERIFY-" + Math.abs(booking.getBookingReference().hashCode() % 1000000));

        if (booking.getUser() != null) {
            ticket.setCustomerName(booking.getUser().getFullName());
            ticket.setCustomerEmail(booking.getUser().getEmail());
        }

        if (booking.getShow() != null) {
            ticket.setShowDate(booking.getShow().getShowDate());
            ticket.setStartTime(booking.getShow().getStartTime());
            ticket.setEndTime(booking.getShow().getEndTime());

            if (booking.getShow().getMovie() != null) {
                ticket.setMovieTitle(booking.getShow().getMovie().getTitle());
                ticket.setMoviePoster(booking.getShow().getMovie().getPosterUrl());
                ticket.setMovieGenre(booking.getShow().getMovie().getGenre());
                ticket.setMovieLanguage(booking.getShow().getMovie().getLanguage());
                ticket.setDurationMinutes(booking.getShow().getMovie().getDurationMinutes());
            }

            if (booking.getShow().getScreen() != null) {
                ticket.setScreenName(booking.getShow().getScreen().getScreenName());
                ticket.setScreenType(booking.getShow().getScreen().getScreenType());

                if (booking.getShow().getScreen().getTheatre() != null) {
                    ticket.setTheatreName(booking.getShow().getScreen().getTheatre().getName());
                    ticket.setTheatreLocation(booking.getShow().getScreen().getTheatre().getLocation());
                    ticket.setTheatreAddress(booking.getShow().getScreen().getTheatre().getAddress());
                }
            }
        }

        ticket.setTotalAmount(booking.getTotalAmount());
        ticket.setBookingStatus(booking.getBookingStatus());
        ticket.setPaymentStatus(paymentStatus != null ? paymentStatus : "SUCCESS");
        ticket.setBookingTime(booking.getBookingTime());

        if (booking.getBookingSeats() != null) {
            for (BookingSeat bs : booking.getBookingSeats()) {
                if (bs.getSeat() != null) {
                    ticket.getSeatNumbers().add(bs.getSeat().getRowLabel() + bs.getSeat().getSeatNumber());
                }
            }
        }

        return ticket;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMoviePoster() {
        return moviePoster;
    }

    public void setMoviePoster(String moviePoster) {
        this.moviePoster = moviePoster;
    }

    public String getMovieGenre() {
        return movieGenre;
    }

    public void setMovieGenre(String movieGenre) {
        this.movieGenre = movieGenre;
    }

    public String getMovieLanguage() {
        return movieLanguage;
    }

    public void setMovieLanguage(String movieLanguage) {
        this.movieLanguage = movieLanguage;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
    }

    public String getTheatreLocation() {
        return theatreLocation;
    }

    public void setTheatreLocation(String theatreLocation) {
        this.theatreLocation = theatreLocation;
    }

    public String getTheatreAddress() {
        return theatreAddress;
    }

    public void setTheatreAddress(String theatreAddress) {
        this.theatreAddress = theatreAddress;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getScreenType() {
        return screenType;
    }

    public void setScreenType(String screenType) {
        this.screenType = screenType;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}
