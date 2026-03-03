package com.scalemart.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scalemart.api.config.BusinessMetrics;
import com.scalemart.api.domain.OrderEntity;
import com.scalemart.api.domain.Product;
import com.scalemart.api.dto.CreateOrderRequest;
import com.scalemart.api.dto.OrderResponse;
import com.scalemart.api.repository.OrderRepository;
import com.scalemart.api.repository.ProductRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 120;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final BusinessMetrics businessMetrics;

    public OrderService(
        ProductRepository productRepository,
        OrderRepository orderRepository,
        OrderEventProducer orderEventProducer,
        BusinessMetrics businessMetrics) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    public OrderResponse createOrder(String userId, CreateOrderRequest request, String idempotencyKey) {
        long startTime = System.currentTimeMillis();
        
        try {
            String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
            if (normalizedIdempotencyKey != null) {
                Optional<OrderEntity> existingOrder = orderRepository.findByUserIdAndIdempotencyKey(
                    userId,
                    normalizedIdempotencyKey);
                if (existingOrder.isPresent()) {
                    return toResponse(existingOrder.get());
                }
            }

            Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.productId()));

            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

            OrderEntity order = new OrderEntity();
            order.setUserId(userId);
            order.setProduct(product);
            order.setQuantity(request.quantity());
            order.setTotalAmount(totalAmount);
            order.setIdempotencyKey(normalizedIdempotencyKey);
            order.setStatus("PENDING");

            OrderEntity saved;
            try {
                saved = orderRepository.save(order);
            } catch (DataIntegrityViolationException exception) {
                if (normalizedIdempotencyKey == null) {
                    throw exception;
                }

                Optional<OrderEntity> existingOrder = orderRepository.findByUserIdAndIdempotencyKey(
                    userId,
                    normalizedIdempotencyKey);
                if (existingOrder.isPresent()) {
                    log.info(
                        "Duplicate order submission coalesced user={} idempotencyKey={}",
                        userId,
                        normalizedIdempotencyKey);
                    return toResponse(existingOrder.get());
                }

                throw exception;
            }

            orderEventProducer.publish(new OrderEvent(
                saved.getId(),
                userId,
                product.getId(),
                saved.getQuantity(),
                saved.getTotalAmount(),
                saved.getCreatedAt()));

            // Record successful order metrics
            long processingTime = System.currentTimeMillis() - startTime;
            businessMetrics.recordOrderSuccess(processingTime);

            return toResponse(saved);
        } catch (Exception e) {
            // Record failed order metrics
            businessMetrics.recordOrderFailure();
            throw e;
        }
    }

    // Get order history for a user
    public List<OrderResponse> getOrderHistory(String userId) {
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // Get orders by status
    public List<OrderResponse> getOrdersByStatus(String userId, String status) {
        List<OrderEntity> orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return orders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // Get single order by ID
    public OrderResponse getOrderById(Long orderId, String userId) {
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order not found");
        }
        
        return toResponse(order);
    }

    // Fallback method for circuit breaker
    public OrderResponse createOrderFallback(String userId, CreateOrderRequest request, String idempotencyKey, Throwable t) {
        log.error("Circuit breaker fallback triggered for order creation: {}", t.getMessage());
        // Record failure when circuit breaker triggers
        businessMetrics.recordOrderFailure();
        throw new RuntimeException("Service temporarily unavailable. Please try again later.");
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String normalized = idempotencyKey.trim();
        if (normalized.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException(
                "Idempotency-Key must be at most " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }

        return normalized;
    }

    private OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(
            order.getId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCreatedAt());
    }
}
