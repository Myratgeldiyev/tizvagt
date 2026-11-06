package com.tizvagt.api.common;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName) {
        super("Not enough stock for: " + productName);
    }
}
