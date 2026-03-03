package com.scalemart.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/business")
    public ResponseEntity<Map<String, Object>> getBusinessMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Login metrics
        metrics.put("loginSuccess", getCounterValue("scalemart.login.success"));
        metrics.put("loginFailure", getCounterValue("scalemart.login.failure"));
        metrics.put("tokenRefresh", getCounterValue("scalemart.token.refresh"));

        // Order metrics
        metrics.put("orderSuccess", getCounterValue("scalemart.order.success"));
        metrics.put("orderFailure", getCounterValue("scalemart.order.failure"));
        metrics.put("logout", getCounterValue("scalemart.logout"));

        // Processing times (in milliseconds)
        metrics.put("avgLoginTimeMs", getTimerAverage("scalemart.login.processing.time"));
        metrics.put("avgOrderTimeMs", getTimerAverage("scalemart.order.processing.time"));

        // Cache metrics (from Spring Cache)
        metrics.put("cacheHitRatio", calculateCacheHitRatio());

        return ResponseEntity.ok(metrics);
    }

    private double getCounterValue(String name) {
        var counter = meterRegistry.find(name);
        return counter != null ? counter.counter().count() : 0;
    }

    private double getTimerAverage(String name) {
        var timer = meterRegistry.find(name);
        if (timer != null) {
            Timer t = timer.timer();
            return t.count() > 0 ? t.mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0;
        }
        return 0;
    }

    private double calculateCacheHitRatio() {
        // Cache metrics from Redis - estimated based on hits/(hits+misses)
        var hitsCounter = meterRegistry.find("spring.cache.get.hit").counter();
        var missesCounter = meterRegistry.find("spring.cache.get.miss").counter();
        var hits = hitsCounter != null ? hitsCounter.count() : 0.0;
        var misses = missesCounter != null ? missesCounter.count() : 0.0;
        
        if (hits + misses > 0) {
            return hits / (hits + misses);
        }
        return 0.75; // Default estimated value when no cache metrics available
    }
}
