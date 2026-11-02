package com.movieticket.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit 1: Java Fundamentals - Inheritance, Object Composition, Method Overriding.
 */
public class Screen extends BaseEntity {

    private Theatre theatre;
    private String screenName;
    private Integer totalRows = 5;
    private Integer totalSeats = 50;
    private String screenType = "Dolby Atmos 4K";
    private List<Seat> seats = new ArrayList<Seat>();
    private List<Show> shows = new ArrayList<Show>();

    public Screen() {
        super();
    }

    public Screen(Theatre theatre, String screenName, Integer totalRows, Integer totalSeats, String screenType) {
        super();
        this.theatre = theatre;
        this.screenName = screenName;
        this.totalRows = totalRows;
        this.totalSeats = totalSeats;
        this.screenType = screenType;
    }

    @Override
    public String getEntitySummary() {
        return "Screen[id=" + getId() + ", name=" + screenName + ", seats=" + totalSeats + "]";
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
