package com.movieticket.model;

import com.movieticket.enums.ShowStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 1 & Unit 2: Inheritance (extends BaseEntity) and Comparable<Show> for chronological sorting.
 */
public class Show extends BaseEntity implements Comparable<Show> {

    private Movie movie;
    private Screen screen;
    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status = ShowStatus.SCHEDULED;
    private Double priceMultiplier = 1.0;
    private List<Booking> bookings = new ArrayList<Booking>();

    public Show() {
        super();
    }

    public Show(Movie movie, Screen screen, LocalDate showDate, LocalTime startTime, LocalTime endTime, String status) {
        super();
        this.movie = movie;
        this.screen = screen;
        this.showDate = showDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = (status != null) ? status : ShowStatus.SCHEDULED;
        this.priceMultiplier = 1.0;
    }

    @Override
    public int compareTo(Show other) {
        if (other == null) {
            return -1;
        }
        if (this.showDate != null && other.showDate != null) {
            int dateCmp = this.showDate.compareTo(other.showDate);
            if (dateCmp != 0) {
                return dateCmp;
            }
        }
        if (this.startTime != null && other.startTime != null) {
            return this.startTime.compareTo(other.startTime);
        }
        return 0;
    }

    @Override
    public String getEntitySummary() {
        return "Show[id=" + getId() + ", date=" + showDate + ", start=" + startTime + ", status=" + status + "]";
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getPriceMultiplier() {
        return priceMultiplier != null ? priceMultiplier : 1.0;
    }

    public void setPriceMultiplier(Double priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
}
