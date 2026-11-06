package com.tizvagt.api.cart;

import com.tizvagt.api.cart.dto.AddCartItemRequest;
import com.tizvagt.api.cart.dto.CartResponse;
import com.tizvagt.api.cart.dto.UpdateCartItemRequest;
import com.tizvagt.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.id());
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal AuthenticatedUser user,
                            @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(user.id(), request.productId(), request.quantity());
    }

    @PatchMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long productId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQuantity(user.id(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long productId) {
        return cartService.removeItem(user.id(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal AuthenticatedUser user) {
        cartService.clear(user.id());
    }
}
