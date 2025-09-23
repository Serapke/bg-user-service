package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.model.TokenInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    
    private final SecretKey secretKey;
    private final long accessTokenExpirationInMinutes;
    private final long refreshTokenExpirationInDays;
    
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-expiration:60}") long accessTokenExpirationInMinutes,
                      @Value("${jwt.refresh-token-expiration:7}") long refreshTokenExpirationInDays) {
        // Ensure the secret is long enough for HMAC-SHA256 (at least 32 bytes)
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret must be configured via jwt.secret property");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long for security");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationInMinutes = accessTokenExpirationInMinutes;
        this.refreshTokenExpirationInDays = refreshTokenExpirationInDays;
    }
    
    public TokenInfo generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationInMinutes, ChronoUnit.MINUTES);
        
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
        
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(expiration, ZoneOffset.UTC);
        return new TokenInfo(token, expiresAt);
    }
    
    public TokenInfo generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpirationInDays, ChronoUnit.DAYS);
        
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
        
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(expiration, ZoneOffset.UTC);
        return new TokenInfo(token, expiresAt);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractClaims(token).getSubject());
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get("type", String.class);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}