package com.tizvagt.api.cart.dto;

import com.tizvagt.api.cart.CartItem;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, int totalQuantity, BigDecimal totalAmount) {

    public static CartResponse from(List<CartItem> items) {
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items.stream().map(CartItemResponse::from).toList(), totalQuantity, totalAmount);
    }
}
