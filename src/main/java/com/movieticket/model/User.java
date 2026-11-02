package com.movieticket.model;

import com.movieticket.enums.UserRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit 1: Java Fundamentals - Inheritance (extends BaseEntity), Encapsulation, Constructors, Method Overriding.
 */
public class User extends BaseEntity {

    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String role = UserRole.CUSTOMER;
    private boolean active = true;
    private List<Booking> bookings = new ArrayList<Booking>();

    public User() {
        super();
    }

    public User(String fullName, String email, String phone, String passwordHash, String role) {
        super();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = (role != null) ? role : UserRole.CUSTOMER;
        this.active = true;
    }

    @Override
    public String getEntitySummary() {
        return "User[id=" + getId() + ", email=" + email + ", role=" + role + "]";
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
}
