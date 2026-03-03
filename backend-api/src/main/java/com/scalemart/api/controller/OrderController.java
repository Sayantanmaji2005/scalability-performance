package com.scalemart.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.scalemart.api.dto.CreateOrderRequest;
import com.scalemart.api.dto.OrderResponse;
import com.scalemart.api.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse create(
        @Valid @RequestBody CreateOrderRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        java.security.Principal principal) {
        return orderService.createOrder(principal.getName(), request, idempotencyKey);
    }

    @GetMapping
    public List<OrderResponse> getOrderHistory(java.security.Principal principal) {
        return orderService.getOrderHistory(principal.getName());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(
        @PathVariable Long orderId,
        java.security.Principal principal) {
        return orderService.getOrderById(orderId, principal.getName());
    }

    @GetMapping("/status/{status}")
    public List<OrderResponse> getOrdersByStatus(
        @PathVariable String status,
        java.security.Principal principal) {
        return orderService.getOrdersByStatus(principal.getName(), status);
    }
}
