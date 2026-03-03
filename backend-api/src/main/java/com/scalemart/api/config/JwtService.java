package com.scalemart.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String base64Secret;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generateToken(String subject) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("type", "access")
            .signWith(signingKey())
            .compact();
    }

    public String generateRefreshToken(String subject) {
        Instant now = Instant.now();
        Instant expiry = now.plus(7, ChronoUnit.DAYS);

        return Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .claim("type", "refresh")
            .signWith(signingKey())
            .compact();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().toInstant().isAfter(Instant.now());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "access".equals(claims.get("type"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Instant extractExpiry(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }
}
