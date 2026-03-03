package com.scalemart.api.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerBuilderCustomizer() {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new JdkSerializationRedisSerializer()));

        return (builder) -> builder
            .withCacheConfiguration(
                "productById",
                baseConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(
                "trendingProducts",
                baseConfig.entryTtl(Duration.ofMinutes(2)));
    }
}
