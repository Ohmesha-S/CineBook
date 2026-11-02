package com.movieticket.service.payment;

import com.movieticket.enums.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * Unit 1: Concrete subclass overriding abstract methods of AbstractPaymentProcessor.
 */
@Component
public class UpiPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    public String getSupportedMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    protected String getReferencePrefix() {
        return "UPI-SIM";
    }

    @Override
    protected String getSuccessMessage() {
        return "Instant UPI payment simulation completed successfully.";
    }

    @Override
    protected String getFailureMessage() {
        return "UPI payment simulation failed (Simulated VPA timeout).";
    }
}
