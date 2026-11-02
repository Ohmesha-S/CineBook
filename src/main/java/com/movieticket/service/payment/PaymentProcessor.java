package com.movieticket.service.payment;

import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.PaymentResponse;
import com.movieticket.model.Booking;

/**
 * Unit 1: Java Fundamentals - Interface defining the polymorphic contract for payment channels.
 */
public interface PaymentProcessor {

    String getSupportedMethod();

    PaymentResponse processPayment(Booking booking, PaymentRequest request);
}
