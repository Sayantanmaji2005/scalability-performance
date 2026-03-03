package com.scalemart.api.config;

import java.util.Locale;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Render Postgres exposes connection strings as postgresql://...
 * JDBC drivers expect jdbc:postgresql://..., so we normalize before datasource
 * auto-configuration binds the final URL.
 */
public class RenderPostgresUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "renderPostgresJdbcUrlOverride";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("JDBC_DATABASE_URL"),
                environment.getProperty("DATABASE_URL"));

        if (rawUrl == null) {
            return;
        }

        String jdbcUrl = normalizeToJdbcUrl(rawUrl);
        if (jdbcUrl == null || jdbcUrl.equals(rawUrl)) {
            return;
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of("spring.datasource.url", jdbcUrl)));
    }

    private String firstNonBlank(String... candidates) {
        for (String value : candidates) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeToJdbcUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("jdbc:")) {
            return url;
        }
        if (lower.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }
        if (lower.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
