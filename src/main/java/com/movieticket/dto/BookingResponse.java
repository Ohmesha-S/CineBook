package com.movieticket.dto;

import com.movieticket.enums.BookingStatus;
import com.movieticket.model.Booking;
import com.movieticket.model.BookingSeat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private Long showId;
    private String movieTitle;
    private String moviePoster;
    private String theatreName;
    private String theatreLocation;
    private String screenName;
    private String screenType;
    private LocalDate showDate;
    private LocalTime startTime;
    private List<String> seatNumbers = new ArrayList<String>();
    private Double subtotal;
    private Double bookingFee;
    private Double totalAmount;
    private String bookingStatus;
    private LocalDateTime bookingTime;
    private LocalDateTime cancellationTime;
    private boolean eligibleForCancellation;

    public BookingResponse() {
    }

    public static BookingResponse fromEntity(Booking booking) {
        if (booking == null) {
            return null;
        }
        BookingResponse dto = new BookingResponse();
        dto.setId(booking.getId());
        dto.setBookingReference(booking.getBookingReference());
        if (booking.getUser() != null) {
            dto.setUserId(booking.getUser().getId());
            dto.setCustomerName(booking.getUser().getFullName());
            dto.setCustomerEmail(booking.getUser().getEmail());
        }
        if (booking.getShow() != null) {
            dto.setShowId(booking.getShow().getId());
            dto.setShowDate(booking.getShow().getShowDate());
            dto.setStartTime(booking.getShow().getStartTime());
            if (booking.getShow().getMovie() != null) {
                dto.setMovieTitle(booking.getShow().getMovie().getTitle());
                dto.setMoviePoster(booking.getShow().getMovie().getPosterUrl());
            }
            if (booking.getShow().getScreen() != null) {
                dto.setScreenName(booking.getShow().getScreen().getScreenName());
                dto.setScreenType(booking.getShow().getScreen().getScreenType());
                if (booking.getShow().getScreen().getTheatre() != null) {
                    dto.setTheatreName(booking.getShow().getScreen().getTheatre().getName());
                    dto.setTheatreLocation(booking.getShow().getScreen().getTheatre().getLocation());
                }
            }
        }
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setBookingStatus(booking.getBookingStatus());
        dto.setBookingTime(booking.getBookingTime());
        dto.setCancellationTime(booking.getCancellationTime());

        if (booking.getBookingSeats() != null && !booking.getBookingSeats().isEmpty()) {
            double seatsTotal = 0.0;
            for (BookingSeat bs : booking.getBookingSeats()) {
                if (bs.getSeat() != null) {
                    dto.getSeatNumbers().add(bs.getSeat().getRowLabel() + bs.getSeat().getSeatNumber());
                    if (bs.getPrice() != null) {
                        seatsTotal += bs.getPrice();
                    }
                }
            }
            dto.setSubtotal(seatsTotal);
            double total = booking.getTotalAmount() != null ? booking.getTotalAmount() : seatsTotal;
            dto.setBookingFee(Math.max(0.0, total - seatsTotal));
        } else {
            dto.setSubtotal(booking.getTotalAmount());
            dto.setBookingFee(0.0);
        }

        boolean eligible = false;
        if (BookingStatus.CONFIRMED.equals(booking.getBookingStatus())
                && booking.getShow() != null
                && booking.getShow().getShowDate() != null
                && booking.getShow().getStartTime() != null) {
            LocalDateTime showDateTime = LocalDateTime.of(booking.getShow().getShowDate(), booking.getShow().getStartTime());
            eligible = LocalDateTime.now().isBefore(showDateTime);
        }
        dto.setEligibleForCancellation(eligible);

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
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

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getBookingFee() {
        return bookingFee;
    }

    public void setBookingFee(Double bookingFee) {
        this.bookingFee = bookingFee;
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

    public boolean isEligibleForCancellation() {
        return eligibleForCancellation;
    }

    public void setEligibleForCancellation(boolean eligibleForCancellation) {
        this.eligibleForCancellation = eligibleForCancellation;
    }
}
