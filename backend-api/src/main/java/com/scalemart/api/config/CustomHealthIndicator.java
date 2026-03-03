package com.scalemart.api.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CustomHealthIndicator(
            DataSource dataSource,
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        try {
            // Check database
            try (Connection connection = dataSource.getConnection()) {
                builder.withDetail("database", "UP");
            }
        } catch (Exception e) {
            builder.down().withDetail("database", "DOWN: " + e.getMessage());
            return builder.build();
        }

        try {
            // Check Redis
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection().ping();
            if ("PONG".equals(pong)) {
                builder.withDetail("redis", "UP");
            } else {
                builder.down().withDetail("redis", "UNEXPECTED: " + pong);
            }
        } catch (Exception e) {
            builder.withDetail("redis", "DOWN: " + e.getMessage());
        }

        try {
            // Check Kafka
            kafkaTemplate.getDefaultTopic();
            builder.withDetail("kafka", "UP");
        } catch (Exception e) {
            builder.withDetail("kafka", "DOWN: " + e.getMessage());
        }

        return builder.up().build();
    }
}
