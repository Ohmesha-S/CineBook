package com.movieticket.enums;

/**
 * Unit 1: Java Fundamentals - Final Class and Static Final Constants.
 */
public final class BookingStatus {

    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String EXPIRED = "EXPIRED";

    private BookingStatus() {
    }

    public static boolean isValid(String status) {
        return PENDING_PAYMENT.equals(status)
                || CONFIRMED.equals(status)
                || CANCELLED.equals(status)
                || PAYMENT_FAILED.equals(status)
                || EXPIRED.equals(status);
    }
}
