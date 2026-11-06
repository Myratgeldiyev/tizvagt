package com.tizvagt.api.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull Long productId,
        @Min(1) @Max(99) int quantity) {
}
