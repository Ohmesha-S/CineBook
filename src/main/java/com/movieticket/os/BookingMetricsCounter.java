package com.movieticket.os;

import org.springframework.stereotype.Component;

/**
 * Unit 6: Java for Operating System - Race Condition Counter Example & Synchronized Methods.
 * Protects shared booking, payment, lock, and socket thread counters from race conditions
 * when accessed concurrently by multiple HTTP request threads and TCP socket worker threads.
 */
@Component
public class BookingMetricsCounter {

    private int bookingAttempts = 0;
    private int confirmedBookings = 0;
    private int cancelledBookings = 0;
    private int paymentTransactions = 0;
    private int activeSocketClients = 0;
    private int totalSocketConnections = 0;
    private int orderedLockAcquisitions = 0;

    public synchronized void incrementBookingAttempts() {
        bookingAttempts = bookingAttempts + 1;
    }

    public synchronized void incrementConfirmedBookings() {
        confirmedBookings = confirmedBookings + 1;
    }

    public synchronized void incrementCancelledBookings() {
        cancelledBookings = cancelledBookings + 1;
    }

    public synchronized void incrementPaymentTransactions() {
        paymentTransactions = paymentTransactions + 1;
    }

    public synchronized void registerSocketClientConnected() {
        activeSocketClients = activeSocketClients + 1;
        totalSocketConnections = totalSocketConnections + 1;
    }

    public synchronized void registerSocketClientDisconnected() {
        if (activeSocketClients > 0) {
            activeSocketClients = activeSocketClients - 1;
        }
    }

    public synchronized void incrementOrderedLockAcquisitions() {
        orderedLockAcquisitions = orderedLockAcquisitions + 1;
    }

    public synchronized int getBookingAttempts() {
        return bookingAttempts;
    }

    public synchronized int getConfirmedBookings() {
        return confirmedBookings;
    }

    public synchronized int getCancelledBookings() {
        return cancelledBookings;
    }

    public synchronized int getPaymentTransactions() {
        return paymentTransactions;
    }

    public synchronized int getActiveSocketClients() {
        return activeSocketClients;
    }

    public synchronized int getTotalSocketConnections() {
        return totalSocketConnections;
    }

    public synchronized int getOrderedLockAcquisitions() {
        return orderedLockAcquisitions;
    }
}

