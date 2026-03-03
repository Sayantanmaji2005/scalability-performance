package com.scalemart.api.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scalemart.api.domain.Product;
import com.scalemart.api.domain.Wishlist;
import com.scalemart.api.dto.WishlistResponse;
import com.scalemart.api.repository.ProductRepository;
import com.scalemart.api.repository.WishlistRepository;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public WishlistResponse addToWishlist(String userId, Long productId) {
        // Check if product exists
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        
        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new IllegalArgumentException("Product already in wishlist");
        }
        
        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);
        wishlist.setProduct(product);
        
        Wishlist saved = wishlistRepository.save(wishlist);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(String userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return wishlists.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void removeFromWishlist(String userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(String userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        return new WishlistResponse(
            wishlist.getId(),
            wishlist.getProduct().getId(),
            wishlist.getProduct().getName(),
            wishlist.getProduct().getCategory(),
            wishlist.getProduct().getPrice(),
            wishlist.getCreatedAt()
        );
    }
}
