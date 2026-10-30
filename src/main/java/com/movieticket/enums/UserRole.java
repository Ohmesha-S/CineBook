package com.movieticket.enums;

public enum UserRole {
    CUSTOMER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
