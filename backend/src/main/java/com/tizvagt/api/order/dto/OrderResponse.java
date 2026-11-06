package com.tizvagt.api.order.dto;

import com.tizvagt.api.order.Order;
import com.tizvagt.api.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        String paymentReference,
        Instant createdAt,
        Instant paidAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPaymentReference(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
