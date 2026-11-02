package com.movieticket.service.payment;

import com.movieticket.enums.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * Unit 1: Concrete subclass overriding abstract methods of AbstractPaymentProcessor.
 */
@Component
public class CardPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    public String getSupportedMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    protected String getReferencePrefix() {
        return "CARD-SIM";
    }

    @Override
    protected String getSuccessMessage() {
        return "Credit/Debit Card payment simulation completed successfully.";
    }

    @Override
    protected String getFailureMessage() {
        return "Card payment simulation failed (Simulated bank decline).";
    }
}
