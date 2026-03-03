package com.scalemart.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration REDIS_RETRY_COOLDOWN = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private volatile Instant redisBypassUntil = Instant.EPOCH;
    private volatile boolean redisBypassLogged;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!isRateLimitedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        int maxPerMinute;
        if (path.contains("/auth/login")) {
            maxPerMinute = 20;
        } else if (path.contains("/auth/register")) {
            maxPerMinute = 10;
        } else if (path.contains("/auth/forgot-password")) {
            maxPerMinute = 10;
        } else if (path.contains("/auth/reset-password")) {
            maxPerMinute = 12;
        } else if (path.contains("/auth/verify-email")) {
            maxPerMinute = 20;
        } else if (path.contains("/auth/resend-verification")) {
            maxPerMinute = 12;
        } else if (path.contains("/auth/change-password")) {
            maxPerMinute = 20;
        } else {
            maxPerMinute = 120;
        }
        String client = resolveClientId(request);
        long window = Instant.now().getEpochSecond() / 60;
        String key = "rl:" + path + ":" + client + ":" + window;
        Instant now = Instant.now();

        Long count;
        if (now.isBefore(redisBypassUntil)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(70));
            }

            if (redisBypassLogged) {
                log.info("Rate limiting backend recovered; Redis connectivity restored");
                redisBypassLogged = false;
            }
        } catch (RuntimeException ex) {
            // Fail open when Redis is unavailable so API traffic is not fully blocked.
            redisBypassUntil = now.plus(REDIS_RETRY_COOLDOWN);
            String error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            if (!redisBypassLogged) {
                log.warn(
                    "Rate limiting unavailable; bypassing for {}s path={} client={} ({})",
                    REDIS_RETRY_COOLDOWN.toSeconds(),
                    path,
                    client,
                    error);
                redisBypassLogged = true;
            } else {
                log.debug(
                    "Rate limiting still unavailable; bypassing path={} client={} ({})",
                    path,
                    client,
                    error);
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (count != null && count > maxPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        return path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/auth/verify-email")
            || path.equals("/api/v1/auth/resend-verification")
            || path.equals("/api/v1/auth/forgot-password")
            || path.equals("/api/v1/auth/reset-password")
            || path.equals("/api/v1/auth/change-password")
            || path.equals("/api/v1/orders");
    }

    private String resolveClientId(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
