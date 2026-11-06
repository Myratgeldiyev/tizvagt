package com.tizvagt.api.payment;

import java.math.BigDecimal;

/**
 * Abstraction over a payment provider. Swapping the mock for Stripe/Iyzico
 * later only needs a new implementation of this interface.
 */
public interface PaymentGateway {

    /** Charges the amount and returns the provider's transaction reference. */
    String charge(Long orderId, BigDecimal amount);
}
