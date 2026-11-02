package com.movieticket.enums;

/**
 * Unit 1: Java Fundamentals - Final Class and Static Final Constants.
 */
public final class MovieStatus {

    public static final String NOW_SHOWING = "NOW_SHOWING";
    public static final String UPCOMING = "UPCOMING";
    public static final String INACTIVE = "INACTIVE";

    private MovieStatus() {
    }

    public static boolean isValid(String status) {
        return NOW_SHOWING.equals(status)
                || UPCOMING.equals(status)
                || INACTIVE.equals(status);
    }
}
