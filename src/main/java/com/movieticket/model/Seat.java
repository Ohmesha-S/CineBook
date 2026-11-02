package com.movieticket.model;

import com.movieticket.enums.SeatCategory;

/**
 * Unit 1 & Unit 2: Inheritance (extends BaseEntity) and Comparable<Seat> for natural row/seat ordering.
 */
public class Seat extends BaseEntity implements Comparable<Seat> {

    private Screen screen;
    private String rowLabel;
    private Integer seatNumber;
    private String seatCategory = SeatCategory.REGULAR;
    private Double basePrice = 150.0;
    private boolean active = true;

    public Seat() {
        super();
    }

    public Seat(Screen screen, String rowLabel, Integer seatNumber, String seatCategory, Double basePrice) {
        super();
        this.screen = screen;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatCategory = (seatCategory != null) ? seatCategory : SeatCategory.REGULAR;
        this.basePrice = basePrice;
        this.active = true;
    }

    @Override
    public int compareTo(Seat other) {
        if (other == null) {
            return -1;
        }
        String r1 = (this.rowLabel != null) ? this.rowLabel : "";
        String r2 = (other.rowLabel != null) ? other.rowLabel : "";
        int rowCmp = r1.compareToIgnoreCase(r2);
        if (rowCmp != 0) {
            return rowCmp;
        }
        Integer n1 = (this.seatNumber != null) ? this.seatNumber : Integer.valueOf(0);
        Integer n2 = (other.seatNumber != null) ? other.seatNumber : Integer.valueOf(0);
        return n1.compareTo(n2);
    }

    @Override
    public String getEntitySummary() {
        return "Seat[id=" + getId() + ", label=" + getSeatIdentifier() + ", category=" + seatCategory + "]";
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatCategory() {
        return seatCategory;
    }

    public void setSeatCategory(String seatCategory) {
        this.seatCategory = seatCategory;
    }

    public Double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Double basePrice) {
        this.basePrice = basePrice;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSeatIdentifier() {
        return rowLabel + seatNumber;
    }
}
