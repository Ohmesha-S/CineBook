package com.movieticket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screens", indexes = {
    @Index(name = "idx_screen_theatre", columnList = "theatre_id")
})
public class Screen extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    @NotBlank(message = "Screen name is required")
    @Column(name = "screen_name", nullable = false, length = 60)
    private String screenName;

    @Column(name = "total_rows")
    private Integer totalRows = 5;

    @Column(name = "total_seats")
    private Integer totalSeats = 50;

    @Column(name = "screen_type", length = 50)
    private String screenType = "Dolby Atmos 4K";

    @JsonIgnore
    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Show> shows = new ArrayList<>();

    public Screen() {
    }

    public Screen(Theatre theatre, String screenName, Integer totalRows, Integer totalSeats, String screenType) {
        this.theatre = theatre;
        this.screenName = screenName;
        this.totalRows = totalRows;
        this.totalSeats = totalSeats;
        this.screenType = screenType;
    }

    public Theatre getTheatre() {
        return theatre;
    }

    public void setTheatre(Theatre theatre) {
        this.theatre = theatre;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getScreenType() {
        return screenType;
    }

    public void setScreenType(String screenType) {
        this.screenType = screenType;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public List<Show> getShows() {
        return shows;
    }

    public void setShows(List<Show> shows) {
        this.shows = shows;
    }
}
