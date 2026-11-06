package com.tizvagt.api.cart.dto;

import com.tizvagt.api.cart.CartItem;
import com.tizvagt.api.catalog.Product;
import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String name,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        int availableStock) {

    public static CartItemResponse from(CartItem item) {
        Product product = item.getProduct();
        return new CartItemResponse(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                item.getQuantity(),
                item.lineTotal(),
                product.getStock());
    }
}
