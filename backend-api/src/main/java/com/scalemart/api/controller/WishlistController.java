package com.scalemart.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.scalemart.api.dto.WishlistResponse;
import com.scalemart.api.service.WishlistService;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<WishlistResponse> getWishlist(java.security.Principal principal) {
        return wishlistService.getWishlist(principal.getName());
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistResponse addToWishlist(
        @PathVariable Long productId,
        java.security.Principal principal) {
        return wishlistService.addToWishlist(principal.getName(), productId);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWishlist(
        @PathVariable Long productId,
        java.security.Principal principal) {
        wishlistService.removeFromWishlist(principal.getName(), productId);
    }
}
