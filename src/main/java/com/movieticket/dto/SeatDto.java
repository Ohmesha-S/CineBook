package com.movieticket.dto;

import com.movieticket.model.Seat;

public class SeatDto {
    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private String seatCategory;
    private Double price;
    private String status;
    private boolean active;

    public SeatDto() {
    }

    public SeatDto(Long id, String rowLabel, Integer seatNumber, String seatCategory, Double price, String status, boolean active) {
        this.id = id;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatCategory = seatCategory;
        this.price = price;
        this.status = status;
        this.active = active;
    }

    public static SeatDto fromEntity(Seat seat, Double effectivePrice, String status) {
        if (seat == null) {
            return null;
        }
        return new SeatDto(
                seat.getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.getSeatCategory(),
                effectivePrice != null ? effectivePrice : seat.getBasePrice(),
                status != null ? status : "AVAILABLE",
                seat.isActive()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getIdentifier() {
        return rowLabel + seatNumber;
    }
}
