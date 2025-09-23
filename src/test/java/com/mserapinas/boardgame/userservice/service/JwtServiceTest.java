package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.model.TokenInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String VALID_SECRET = "mySecretKeyThatIsLongEnoughForHmacSha256AndIsSecure";
    private static final String SHORT_SECRET = "short";
    private static final Long TEST_USER_ID = 123L;
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(VALID_SECRET, 60, 7);
    }

    @Test
    @DisplayName("Should throw exception when JWT secret is null")
    void shouldThrowExceptionWhenSecretIsNull() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new JwtService(null, 60, 7)
        );
        assertEquals("JWT secret must be configured via jwt.secret property", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when JWT secret is empty")
    void shouldThrowExceptionWhenSecretIsEmpty() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new JwtService("", 60, 7)
        );
        assertEquals("JWT secret must be configured via jwt.secret property", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when JWT secret is too short")
    void shouldThrowExceptionWhenSecretIsTooShort() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new JwtService(SHORT_SECRET, 60, 7)
        );
        assertEquals("JWT secret must be at least 32 characters long for security", exception.getMessage());
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        TokenInfo tokenInfo = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        
        assertNotNull(tokenInfo);
        assertNotNull(tokenInfo.token());
        assertNotNull(tokenInfo.expiresAt());
        assertTrue(tokenInfo.expiresAt().isAfter(OffsetDateTime.now()));
        assertTrue(jwtService.isTokenValid(tokenInfo.token()));
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        TokenInfo tokenInfo = jwtService.generateRefreshToken(TEST_USER_ID);
        
        assertNotNull(tokenInfo);
        assertNotNull(tokenInfo.token());
        assertNotNull(tokenInfo.expiresAt());
        assertTrue(tokenInfo.expiresAt().isAfter(OffsetDateTime.now()));
        assertTrue(jwtService.isTokenValid(tokenInfo.token()));
    }

    @Test
    @DisplayName("Should extract user ID from access token")
    void shouldExtractUserIdFromAccessToken() {
        TokenInfo tokenInfo = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        
        Long extractedUserId = jwtService.extractUserId(tokenInfo.token());
        
        assertEquals(TEST_USER_ID, extractedUserId);
    }

    @Test
    @DisplayName("Should extract email from access token")
    void shouldExtractEmailFromAccessToken() {
        TokenInfo tokenInfo = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        
        String extractedEmail = jwtService.extractEmail(tokenInfo.token());
        
        assertEquals(TEST_EMAIL, extractedEmail);
    }

    @Test
    @DisplayName("Should extract token type from access token")
    void shouldExtractTokenTypeFromAccessToken() {
        TokenInfo tokenInfo = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        
        String tokenType = jwtService.extractTokenType(tokenInfo.token());
        
        assertEquals("access", tokenType);
    }

    @Test
    @DisplayName("Should extract token type from refresh token")
    void shouldExtractTokenTypeFromRefreshToken() {
        TokenInfo tokenInfo = jwtService.generateRefreshToken(TEST_USER_ID);
        
        String tokenType = jwtService.extractTokenType(tokenInfo.token());
        
        assertEquals("refresh", tokenType);
    }

    @Test
    @DisplayName("Should validate valid token as true")
    void shouldValidateValidTokenAsTrue() {
        TokenInfo tokenInfo = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        
        boolean isValid = jwtService.isTokenValid(tokenInfo.token());
        
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should validate malformed token as false")
    void shouldValidateMalformedTokenAsFalse() {
        String malformedToken = "invalid.token.here";
        
        boolean isValid = jwtService.isTokenValid(malformedToken);
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate empty token as false")
    void shouldValidateEmptyTokenAsFalse() {
        boolean isValid = jwtService.isTokenValid("");
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate null token as false")
    void shouldValidateNullTokenAsFalse() {
        boolean isValid = jwtService.isTokenValid(null);
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle token with different secret as invalid")
    void shouldHandleTokenWithDifferentSecretAsInvalid() {
        JwtService otherJwtService = new JwtService("differentSecretKeyThatIsLongEnough", 60, 7);
        TokenInfo tokenInfo = otherJwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        
        boolean isValid = jwtService.isTokenValid(tokenInfo.token());
        
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        Long userId1 = 123L;
        Long userId2 = 456L;
        
        TokenInfo token1 = jwtService.generateAccessToken(userId1, "user1@example.com");
        TokenInfo token2 = jwtService.generateAccessToken(userId2, "user2@example.com");
        
        assertNotEquals(token1.token(), token2.token());
    }

    @Test
    @DisplayName("Should set correct expiration time for access token")
    void shouldSetCorrectExpirationTimeForAccessToken() {
        int customExpirationMinutes = 30;
        JwtService customJwtService = new JwtService(VALID_SECRET, customExpirationMinutes, 7);
        
        OffsetDateTime beforeGeneration = OffsetDateTime.now();
        TokenInfo tokenInfo = customJwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        OffsetDateTime afterGeneration = OffsetDateTime.now();
        
        OffsetDateTime expectedMinExpiration = beforeGeneration.plusMinutes(customExpirationMinutes);
        OffsetDateTime expectedMaxExpiration = afterGeneration.plusMinutes(customExpirationMinutes);
        
        assertTrue(tokenInfo.expiresAt().isAfter(expectedMinExpiration.minusSeconds(1)));
        assertTrue(tokenInfo.expiresAt().isBefore(expectedMaxExpiration.plusSeconds(1)));
    }

    @Test
    @DisplayName("Should set correct expiration time for refresh token")
    void shouldSetCorrectExpirationTimeForRefreshToken() {
        int customExpirationDays = 14;
        JwtService customJwtService = new JwtService(VALID_SECRET, 60, customExpirationDays);
        
        OffsetDateTime beforeGeneration = OffsetDateTime.now();
        TokenInfo tokenInfo = customJwtService.generateRefreshToken(TEST_USER_ID);
        OffsetDateTime afterGeneration = OffsetDateTime.now();
        
        OffsetDateTime expectedMinExpiration = beforeGeneration.plusDays(customExpirationDays);
        OffsetDateTime expectedMaxExpiration = afterGeneration.plusDays(customExpirationDays);
        
        assertTrue(tokenInfo.expiresAt().isAfter(expectedMinExpiration.minusSeconds(1)));
        assertTrue(tokenInfo.expiresAt().isBefore(expectedMaxExpiration.plusSeconds(1)));
    }
}