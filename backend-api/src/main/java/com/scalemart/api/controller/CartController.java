package com.scalemart.api.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scalemart.api.domain.Cart;
import com.scalemart.api.domain.CartItem;
import com.scalemart.api.dto.CartResponse;
import com.scalemart.api.service.CartService;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        Cart cart = cartService.getCart(userId);
        return ResponseEntity.ok(toResponse(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        Long userId = getUserId(authentication);
        Cart cart = cartService.addItem(userId, productId, quantity);
        return ResponseEntity.ok(toResponse(cart));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            Authentication authentication,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        Long userId = getUserId(authentication);
        Cart cart = cartService.updateItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(toResponse(cart));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            Authentication authentication,
            @PathVariable Long productId) {
        Long userId = getUserId(authentication);
        Cart cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(toResponse(cart));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getCartTotal(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(cartService.getCartTotal(userId));
    }

    private Long getUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
            .map(item -> new CartResponse.CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()))
            .collect(Collectors.toList());
        
        BigDecimal total = cart.getItems().stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int itemCount = cart.getItems().stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
        
        return new CartResponse(cart.getId(), cart.getUserId(), itemResponses, total, itemCount);
    }
}
