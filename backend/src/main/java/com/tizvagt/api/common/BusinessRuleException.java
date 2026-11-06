package com.tizvagt.api.common;

/**
 * The request is valid JSON, but breaks a business rule (e.g. "cart is empty", "order already cancelled").
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
