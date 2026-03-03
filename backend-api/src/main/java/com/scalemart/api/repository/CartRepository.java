package com.scalemart.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scalemart.api.domain.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
