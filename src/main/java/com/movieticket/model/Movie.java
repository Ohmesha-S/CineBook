package com.movieticket.model;

import com.movieticket.enums.MovieStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit 1 & Unit 2: Inheritance (extends BaseEntity) and Comparable<Movie> interface for natural sorting.
 */
public class Movie extends BaseEntity implements Comparable<Movie> {

    private String title;
    private String description;
    private String genre;
    private String language;
    private Integer durationMinutes;
    private String rating;
    private LocalDate releaseDate;
    private String posterUrl;
    private String backdropUrl;
    private String director;
    private String cast;
    private String status = MovieStatus.NOW_SHOWING;
    private List<Show> shows = new ArrayList<Show>();

    public Movie() {
        super();
    }

    public Movie(String title, String description, String genre, String language,
                 Integer durationMinutes, String rating, LocalDate releaseDate,
                 String posterUrl, String backdropUrl, String director, String cast,
                 String status) {
        super();
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.language = language;
        this.durationMinutes = durationMinutes;
        this.rating = rating;
        this.releaseDate = releaseDate;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        this.director = director;
        this.cast = cast;
        this.status = (status != null) ? status : MovieStatus.NOW_SHOWING;
    }

    @Override
    public int compareTo(Movie other) {
        if (other == null) {
            return -1;
        }
        if (this.releaseDate != null && other.releaseDate != null) {
            int dateCmp = other.releaseDate.compareTo(this.releaseDate);
            if (dateCmp != 0) {
                return dateCmp;
            }
        } else if (this.releaseDate != null) {
            return -1;
        } else if (other.releaseDate != null) {
            return 1;
        }
        String t1 = (this.title != null) ? this.title : "";
        String t2 = (other.title != null) ? other.title : "";
        return t1.compareToIgnoreCase(t2);
    }

    @Override
    public String getEntitySummary() {
        return "Movie[id=" + getId() + ", title=" + title + ", status=" + status + "]";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        this.backdropUrl = backdropUrl;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Show> getShows() {
        return shows;
    }

    public void setShows(List<Show> shows) {
        this.shows = shows;
    }
}
