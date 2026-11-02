package com.movieticket.enums;

/**
 * Unit 1: Java Fundamentals - Final Class and Static Final Constants.
 */
public final class PaymentMethod {

    public static final String UPI = "UPI";
    public static final String CARD = "CARD";
    public static final String NET_BANKING = "NET_BANKING";

    private PaymentMethod() {
    }

    public static boolean isValid(String method) {
        return UPI.equals(method)
                || CARD.equals(method)
                || NET_BANKING.equals(method);
    }
}
