package com.tizvagt.api.cart;

import com.tizvagt.api.cart.dto.CartResponse;
import com.tizvagt.api.catalog.Product;
import com.tizvagt.api.catalog.ProductRepository;
import com.tizvagt.api.common.InsufficientStockException;
import com.tizvagt.api.common.ResourceNotFoundException;
import com.tizvagt.api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return CartResponse.from(cartItemRepository.findByUserIdOrderByProductIdAsc(userId));
    }

    /** Adds the product, or increases its quantity if it is already in the cart. */
    @Transactional
    public CartResponse addItem(Long userId, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        cartItemRepository.findByUserIdAndProductId(userId, productId)
                .ifPresentOrElse(
                        item -> item.changeQuantity(checkStock(product, item.getQuantity() + quantity)),
                        () -> cartItemRepository.save(new CartItem(
                                // getReferenceById: we only need the foreign key, so no SELECT is run for the user.
                                userRepository.getReferenceById(userId), product, checkStock(product, quantity))));

        return getCart(userId);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long productId, int quantity) {
        CartItem item = findItem(userId, productId);
        item.changeQuantity(checkStock(item.getProduct(), quantity));
        return getCart(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        cartItemRepository.delete(findItem(userId, productId));
        return getCart(userId);
    }

    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }

    private CartItem findItem(Long userId, Long productId) {
        return cartItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", productId));
    }

    private static int checkStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(product.getName());
        }
        return quantity;
    }
}
