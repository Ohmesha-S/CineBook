package com.movieticket.enums;

/**
 * Unit 1: Java Fundamentals - Final Class and Static Final Constants.
 */
public final class PaymentStatus {

    public static final String INITIATED = "INITIATED";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private PaymentStatus() {
    }

    public static boolean isValid(String status) {
        return INITIATED.equals(status)
                || SUCCESS.equals(status)
                || FAILED.equals(status);
    }
}
