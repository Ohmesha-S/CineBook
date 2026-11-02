package com.movieticket.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit 1: Java Fundamentals - Inheritance, Constructors, Encapsulation, Method Overriding.
 */
public class Theatre extends BaseEntity {

    private String name;
    private String location;
    private String address;
    private String contactNumber;
    private String status = "ACTIVE";
    private int screenCount = 0;
    private List<Screen> screens = new ArrayList<Screen>();

    public Theatre() {
        super();
    }

    public Theatre(String name, String location, String address, String contactNumber, String status) {
        super();
        this.name = name;
        this.location = location;
        this.address = address;
        this.contactNumber = contactNumber;
        this.status = (status != null) ? status : "ACTIVE";
    }

    @Override
    public String getEntitySummary() {
        return "Theatre[id=" + getId() + ", name=" + name + ", location=" + location + "]";
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

    public int getScreenCount() {
        if (screens != null && !screens.isEmpty()) {
            return screens.size();
        }
        return screenCount;
    }

    public void setScreenCount(int screenCount) {
        this.screenCount = screenCount;
    }

    public List<Screen> getScreens() {
        return screens;
    }

    public void setScreens(List<Screen> screens) {
        this.screens = screens;
    }
}
