package com.movieticket.enums;

/**
 * Unit 1: Java Fundamentals - Final Class and Static Final Constants.
 */
public final class SeatCategory {

    public static final String REGULAR = "REGULAR";
    public static final String PREMIUM = "PREMIUM";
    public static final String RECLINER = "RECLINER";

    public static final double REGULAR_PRICE = 150.0;
    public static final double PREMIUM_PRICE = 250.0;
    public static final double RECLINER_PRICE = 400.0;

    private SeatCategory() {
    }

    public static double getDefaultPrice(String category) {
        if (RECLINER.equalsIgnoreCase(category)) {
            return RECLINER_PRICE;
        }
        if (PREMIUM.equalsIgnoreCase(category)) {
            return PREMIUM_PRICE;
        }
        return REGULAR_PRICE;
    }

    public static boolean isValid(String category) {
        return REGULAR.equals(category)
                || PREMIUM.equals(category)
                || RECLINER.equals(category);
    }
}
