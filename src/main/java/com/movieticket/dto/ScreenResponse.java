package com.movieticket.dto;

import com.movieticket.model.Screen;

public class ScreenResponse {
    private Long id;
    private Long theatreId;
    private String theatreName;
    private String screenName;
    private Integer totalRows;
    private Integer totalSeats;
    private String screenType;

    public ScreenResponse() {
    }

    public static ScreenResponse fromEntity(Screen screen) {
        if (screen == null) return null;
        ScreenResponse dto = new ScreenResponse();
        dto.setId(screen.getId());
        if (screen.getTheatre() != null) {
            dto.setTheatreId(screen.getTheatre().getId());
            dto.setTheatreName(screen.getTheatre().getName());
        }
        dto.setScreenName(screen.getScreenName());
        dto.setTotalRows(screen.getTotalRows());
        dto.setTotalSeats(screen.getTotalSeats());
        dto.setScreenType(screen.getScreenType());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTheatreId() {
        return theatreId;
    }

    public void setTheatreId(Long theatreId) {
        this.theatreId = theatreId;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
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
}
