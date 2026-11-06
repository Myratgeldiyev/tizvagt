package com.tizvagt.api.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Always succeeds. No real money is involved.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String charge(Long orderId, BigDecimal amount) {
        return "MOCK-" + UUID.randomUUID();
    }
}
