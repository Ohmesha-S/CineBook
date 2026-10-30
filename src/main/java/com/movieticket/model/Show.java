package com.movieticket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.movieticket.enums.ShowStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shows", indexes = {
    @Index(name = "idx_show_movie", columnList = "movie_id"),
    @Index(name = "idx_show_screen", columnList = "screen_id"),
    @Index(name = "idx_show_date", columnList = "show_date")
})
public class Show extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @NotNull(message = "Show date is required")
    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShowStatus status = ShowStatus.SCHEDULED;

    @Column(name = "price_multiplier")
    private Double priceMultiplier = 1.0;

    @JsonIgnore
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    public Show() {
    }

    public Show(Movie movie, Screen screen, LocalDate showDate, LocalTime startTime, LocalTime endTime, ShowStatus status) {
        this.movie = movie;
        this.screen = screen;
        this.showDate = showDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = (status != null) ? status : ShowStatus.SCHEDULED;
        this.priceMultiplier = 1.0;
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

    public ShowStatus getStatus() {
        return status;
    }

    public void setStatus(ShowStatus status) {
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
