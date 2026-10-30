package com.movieticket.enums;

public enum SeatCategory {
    REGULAR(150.0),
    PREMIUM(250.0),
    RECLINER(400.0);

    private final double defaultPrice;

    SeatCategory(double defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }
}
