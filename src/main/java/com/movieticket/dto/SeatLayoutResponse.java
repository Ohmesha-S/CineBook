package com.movieticket.dto;

import java.util.List;
import java.util.Map;

public class SeatLayoutResponse {
    private Long screenId;
    private String screenName;
    private String theatreName;
    private Long showId;
    private Integer totalRows;
    private Integer totalSeats;
    private List<SeatDto> seats;
    private Map<String, List<SeatDto>> rows;

    public SeatLayoutResponse() {
    }

    public SeatLayoutResponse(Long screenId, String screenName, String theatreName, Long showId,
                              Integer totalRows, Integer totalSeats, List<SeatDto> seats, Map<String, List<SeatDto>> rows) {
        this.screenId = screenId;
        this.screenName = screenName;
        this.theatreName = theatreName;
        this.showId = showId;
        this.totalRows = totalRows;
        this.totalSeats = totalSeats;
        this.seats = seats;
        this.rows = rows;
    }

    public Long getScreenId() {
        return screenId;
    }

    public void setScreenId(Long screenId) {
        this.screenId = screenId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
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

    public List<SeatDto> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatDto> seats) {
        this.seats = seats;
    }

    public Map<String, List<SeatDto>> getRows() {
        return rows;
    }

    public void setRows(Map<String, List<SeatDto>> rows) {
        this.rows = rows;
    }
}
