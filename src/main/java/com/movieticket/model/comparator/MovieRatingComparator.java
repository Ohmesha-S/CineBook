package com.movieticket.model.comparator;

import com.movieticket.model.Movie;

import java.util.Comparator;

/**
 * Unit 2: Java Collections and Data Handling - Concrete Comparator Interface Implementation.
 * Sorts Movie objects by rating descending, then title ascending.
 */
public class MovieRatingComparator implements Comparator<Movie> {

    @Override
    public int compare(Movie m1, Movie m2) {
        if (m1 == m2) {
            return 0;
        }
        if (m1 == null) {
            return 1;
        }
        if (m2 == null) {
            return -1;
        }
        String r1 = (m1.getRating() != null) ? m1.getRating() : "";
        String r2 = (m2.getRating() != null) ? m2.getRating() : "";
        int ratingCmp = r2.compareToIgnoreCase(r1);
        if (ratingCmp != 0) {
            return ratingCmp;
        }
        String t1 = (m1.getTitle() != null) ? m1.getTitle() : "";
        String t2 = (m2.getTitle() != null) ? m2.getTitle() : "";
        return t1.compareToIgnoreCase(t2);
    }
}

