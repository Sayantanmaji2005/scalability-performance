package com.scalemart.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scalemart.api.domain.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
    
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<OrderEntity> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
}
