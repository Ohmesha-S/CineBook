package com.movieticket.enums;

/**
 * Unit 1: Java Fundamentals - Final Class and Static Final Constants.
 */
public final class UserRole {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String ADMIN = "ADMIN";

    private UserRole() {
    }

    public static String getAuthority(String role) {
        return "ROLE_" + role;
    }

    public static boolean isValid(String role) {
        return CUSTOMER.equals(role) || ADMIN.equals(role);
    }
}
