package com.movieticket.dto;

import com.movieticket.model.Theatre;

public class TheatreResponse {
    private Long id;
    private String name;
    private String location;
    private String address;
    private String contactNumber;
    private String status;
    private int screenCount;

    public TheatreResponse() {
    }

    public static TheatreResponse fromEntity(Theatre theatre) {
        if (theatre == null) return null;
        TheatreResponse dto = new TheatreResponse();
        dto.setId(theatre.getId());
        dto.setName(theatre.getName());
        dto.setLocation(theatre.getLocation());
        dto.setAddress(theatre.getAddress());
        dto.setContactNumber(theatre.getContactNumber());
        dto.setStatus(theatre.getStatus());
        try {
            if (theatre.getScreens() != null && org.hibernate.Hibernate.isInitialized(theatre.getScreens())) {
                dto.setScreenCount(theatre.getScreens().size());
            } else {
                dto.setScreenCount(0);
            }
        } catch (Exception e) {
            dto.setScreenCount(0);
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return screenCount;
    }

    public void setScreenCount(int screenCount) {
        this.screenCount = screenCount;
    }
}
