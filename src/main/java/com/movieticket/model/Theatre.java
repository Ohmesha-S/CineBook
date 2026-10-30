package com.movieticket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "theatres", indexes = {
    @Index(name = "idx_theatre_name", columnList = "name"),
    @Index(name = "idx_theatre_location", columnList = "location")
})
public class Theatre extends BaseEntity {

    @NotBlank(message = "Theatre name is required")
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotBlank(message = "Location/City is required")
    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @JsonIgnore
    @OneToMany(mappedBy = "theatre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Screen> screens = new ArrayList<>();

    public Theatre() {
    }

    public Theatre(String name, String location, String address, String contactNumber, String status) {
        this.name = name;
        this.location = location;
        this.address = address;
        this.contactNumber = contactNumber;
        this.status = (status != null) ? status : "ACTIVE";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Screen> getScreens() {
        return screens;
    }

    public void setScreens(List<Screen> screens) {
        this.screens = screens;
    }
}
