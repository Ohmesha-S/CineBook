package com.movieticket.service.payment;

import com.movieticket.enums.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * Unit 1: Concrete subclass overriding abstract methods of AbstractPaymentProcessor.
 */
@Component
public class NetBankingPaymentProcessor extends AbstractPaymentProcessor {

    @Override
    public String getSupportedMethod() {
        return PaymentMethod.NET_BANKING;
    }

    @Override
    protected String getReferencePrefix() {
        return "NB-SIM";
    }

    @Override
    protected String getSuccessMessage() {
        return "Net Banking simulated payment completed successfully.";
    }

    @Override
    protected String getFailureMessage() {
        return "Net Banking payment simulation failed (Simulated bank server error).";
    }
}
