package com.movieticket.model;

import java.time.LocalDateTime;

/**
 * Unit 1: Java Fundamentals - Abstract Class, Inheritance, Protected Members,
 * Overloaded Constructors, Method Overriding, and Dynamic Method Dispatch.
 */
public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected BaseEntity() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    protected BaseEntity(Long id) {
        this();
        this.id = id;
    }

    public void touchForCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    public void touchForUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Abstract method implemented by all subclasses to demonstrate Dynamic Method Dispatch.
     */
    public abstract String getEntitySummary();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return getEntitySummary();
    }
}
