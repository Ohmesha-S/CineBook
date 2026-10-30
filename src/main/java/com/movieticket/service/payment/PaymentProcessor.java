package com.movieticket.service.payment;

import com.movieticket.dto.PaymentRequest;
import com.movieticket.dto.PaymentResponse;
import com.movieticket.enums.PaymentMethod;
import com.movieticket.model.Booking;

/**
 * PaymentProcessor demonstrates OOP Polymorphism and the Strategy Design Pattern.
 * Different simulated payment channels (Card, UPI, Net Banking) implement this contract.
 */
public interface PaymentProcessor {

    PaymentMethod getSupportedMethod();

    PaymentResponse processPayment(Booking booking, PaymentRequest request);
}
