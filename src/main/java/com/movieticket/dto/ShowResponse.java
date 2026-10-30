package com.movieticket.dto;

import com.movieticket.enums.ShowStatus;
import com.movieticket.model.Show;
import java.time.LocalDate;
import java.time.LocalTime;

public class ShowResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private String moviePoster;
    private Integer movieDuration;
    private Long theatreId;
    private String theatreName;
    private String theatreLocation;
    private Long screenId;
    private String screenName;
    private String screenType;
    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ShowStatus status;
    private Double priceMultiplier;

    public ShowResponse() {
    }

    public static ShowResponse fromEntity(Show show) {
        if (show == null) return null;
        ShowResponse dto = new ShowResponse();
        dto.setId(show.getId());
        if (show.getMovie() != null) {
            dto.setMovieId(show.getMovie().getId());
            dto.setMovieTitle(show.getMovie().getTitle());
            dto.setMoviePoster(show.getMovie().getPosterUrl());
            dto.setMovieDuration(show.getMovie().getDurationMinutes());
        }
        if (show.getScreen() != null) {
            dto.setScreenId(show.getScreen().getId());
            dto.setScreenName(show.getScreen().getScreenName());
            dto.setScreenType(show.getScreen().getScreenType());
            if (show.getScreen().getTheatre() != null) {
                dto.setTheatreId(show.getScreen().getTheatre().getId());
                dto.setTheatreName(show.getScreen().getTheatre().getName());
                dto.setTheatreLocation(show.getScreen().getTheatre().getLocation());
            }
        }
        dto.setShowDate(show.getShowDate());
        dto.setStartTime(show.getStartTime());
        dto.setEndTime(show.getEndTime());
        dto.setStatus(show.getStatus());
        dto.setPriceMultiplier(show.getPriceMultiplier());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
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

    public Integer getMovieDuration() {
        return movieDuration;
    }

    public void setMovieDuration(Integer movieDuration) {
        this.movieDuration = movieDuration;
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

    public String getTheatreLocation() {
        return theatreLocation;
    }

    public void setTheatreLocation(String theatreLocation) {
        this.theatreLocation = theatreLocation;
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

    public ShowStatus getStatus() {
        return status;
    }

    public void setStatus(ShowStatus status) {
        this.status = status;
    }

    public Double getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(Double priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }
}
