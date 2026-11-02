package com.movieticket.model.comparator;

import com.movieticket.model.Booking;

import java.util.Comparator;

/**
 * Unit 2: Java Collections and Data Handling - Concrete Comparator Interface Implementation.
 * Sorts Booking objects by bookingTime descending (newest first).
 */
public class BookingTimeDescComparator implements Comparator<Booking> {

    @Override
    public int compare(Booking b1, Booking b2) {
        if (b1 == b2) {
            return 0;
        }
        if (b1 == null) {
            return 1;
        }
        if (b2 == null) {
            return -1;
        }
        if (b1.getBookingTime() != null && b2.getBookingTime() != null) {
            return b2.getBookingTime().compareTo(b1.getBookingTime());
        }
        if (b1.getBookingTime() != null) {
            return -1;
        }
        if (b2.getBookingTime() != null) {
            return 1;
        }
        return 0;
    }
}

