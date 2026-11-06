package com.tizvagt.api.order;

import com.tizvagt.api.cart.CartItem;
import com.tizvagt.api.cart.CartItemRepository;
import com.tizvagt.api.catalog.ProductRepository;
import com.tizvagt.api.common.BusinessRuleException;
import com.tizvagt.api.common.InsufficientStockException;
import com.tizvagt.api.common.PageResponse;
import com.tizvagt.api.common.ResourceNotFoundException;
import com.tizvagt.api.order.dto.OrderResponse;
import com.tizvagt.api.payment.PaymentGateway;
import com.tizvagt.api.user.UserRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final Clock clock = Clock.systemUTC();

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                        ProductRepository productRepository, UserRepository userRepository,
                        PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Turns the cart into a paid order. Everything runs in ONE transaction:
     * if any step throws, stock changes and the order are rolled back and the cart stays as it was.
     */
    @Transactional
    public OrderResponse checkout(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByProductIdAsc(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException("Cart is empty");
        }

        Order order = new Order(userRepository.getReferenceById(userId));
        for (CartItem cartItem : cartItems) {
            int updated = productRepository.decreaseStock(cartItem.getProduct().getId(), cartItem.getQuantity());
            if (updated == 0) {
                throw new InsufficientStockException(cartItem.getProduct().getName());
            }
            order.addItem(new OrderItem(cartItem.getProduct(), cartItem.getQuantity()));
        }
        orderRepository.save(order);

        String paymentReference = paymentGateway.charge(order.getId(), order.getTotalAmount());
        order.markPaid(paymentReference, clock.instant());

        cartItemRepository.deleteAllByUserId(userId);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(Long userId, Pageable pageable) {
        return PageResponse.from(orderRepository.findByUserId(userId, pageable), OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        return OrderResponse.from(findOwnOrder(userId, orderId));
    }

    /** Cancels a paid order and puts the items back in stock. */
    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = findOwnOrder(userId, orderId);
        order.cancel();
        order.getItems().forEach(item -> productRepository.increaseStock(item.getProductId(), item.getQuantity()));
        return OrderResponse.from(order);
    }

    // Someone else's order returns 404, not 403, so ids of other users' orders are not revealed.
    private Order findOwnOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }
}
