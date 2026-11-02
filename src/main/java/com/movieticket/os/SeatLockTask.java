package com.movieticket.os;

/**
 * Unit 1 & Unit 2: Generic Interface representing a critical-section booking task
 * executed while holding ordered seat locks.
 */
public interface SeatLockTask<T> {
    T execute();
}

