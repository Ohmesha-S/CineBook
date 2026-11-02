package com.movieticket.enums;

/**
 * Unit 1: Java Fundamentals - Final Class and Static Final Constants.
 */
public final class ShowStatus {

    public static final String SCHEDULED = "SCHEDULED";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private ShowStatus() {
    }

    public static boolean isValid(String status) {
        return SCHEDULED.equals(status)
                || RUNNING.equals(status)
                || COMPLETED.equals(status)
                || CANCELLED.equals(status);
    }
}
