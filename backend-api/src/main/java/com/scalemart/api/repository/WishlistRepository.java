package com.scalemart.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scalemart.api.domain.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Optional<Wishlist> findByUserIdAndProductId(String userId, Long productId);
    
    void deleteByUserIdAndProductId(String userId, Long productId);
    
    boolean existsByUserIdAndProductId(String userId, Long productId);
}
