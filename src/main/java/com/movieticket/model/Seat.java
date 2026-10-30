package com.movieticket.model;

import com.movieticket.enums.SeatCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_screen_row_seat", columnNames = {"screen_id", "row_label", "seat_number"})
    },
    indexes = {
        @Index(name = "idx_seat_screen", columnList = "screen_id")
    }
)
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @NotBlank(message = "Row label is required")
    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @NotNull(message = "Seat number is required")
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_category", nullable = false, length = 30)
    private SeatCategory seatCategory = SeatCategory.REGULAR;

    @NotNull(message = "Base price is required")
    @Column(name = "base_price", nullable = false)
    private Double basePrice = 150.0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Seat() {
    }

    public Seat(Screen screen, String rowLabel, Integer seatNumber, SeatCategory seatCategory, Double basePrice) {
        this.screen = screen;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatCategory = seatCategory;
        this.basePrice = basePrice;
        this.active = true;
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

    public SeatCategory getSeatCategory() {
        return seatCategory;
    }

    public void setSeatCategory(SeatCategory seatCategory) {
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
