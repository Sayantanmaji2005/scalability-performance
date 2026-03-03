package com.scalemart.api.config;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class BusinessMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter tokenRefreshCounter;
    private final Counter orderSuccessCounter;
    private final Counter orderFailureCounter;
    private final Counter logoutCounter;
    private final Timer orderProcessingTimer;
    private final Timer loginProcessingTimer;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        // Login metrics
        this.loginSuccessCounter = Counter.builder("scalemart.login.success")
                .description("Number of successful login attempts")
                .register(meterRegistry);
        
        this.loginFailureCounter = Counter.builder("scalemart.login.failure")
                .description("Number of failed login attempts")
                .register(meterRegistry);
        
        this.loginProcessingTimer = Timer.builder("scalemart.login.processing.time")
                .description("Time taken for login processing")
                .register(meterRegistry);
        
        // Token metrics
        this.tokenRefreshCounter = Counter.builder("scalemart.token.refresh")
                .description("Number of token refresh operations")
                .register(meterRegistry);
        
        // Order metrics
        this.orderSuccessCounter = Counter.builder("scalemart.order.success")
                .description("Number of successful orders")
                .register(meterRegistry);
        
        this.orderFailureCounter = Counter.builder("scalemart.order.failure")
                .description("Number of failed orders")
                .register(meterRegistry);
        
        this.orderProcessingTimer = Timer.builder("scalemart.order.processing.time")
                .description("Time taken for order processing")
                .register(meterRegistry);
        
        // Logout metrics
        this.logoutCounter = Counter.builder("scalemart.logout")
                .description("Number of logout operations")
                .register(meterRegistry);
    }

    public void recordLoginSuccess(long processingTimeMs) {
        loginSuccessCounter.increment();
        loginProcessingTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    public void recordTokenRefresh() {
        tokenRefreshCounter.increment();
    }

    public void recordOrderSuccess(long processingTimeMs) {
        orderSuccessCounter.increment();
        orderProcessingTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    public void recordOrderFailure() {
        orderFailureCounter.increment();
    }

    public void recordLogout() {
        logoutCounter.increment();
    }
}
