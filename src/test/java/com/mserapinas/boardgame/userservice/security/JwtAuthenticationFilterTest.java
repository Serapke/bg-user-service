package com.mserapinas.boardgame.userservice.security;

import com.mserapinas.boardgame.userservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String BEARER_VALID_TOKEN = "Bearer " + VALID_TOKEN;
    private static final String BEARER_INVALID_TOKEN = "Bearer " + INVALID_TOKEN;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should continue filter chain when no Authorization header present")
    void shouldContinueFilterChainWhenNoAuthorizationHeaderPresent() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(anyString());
        verifyNoInteractions(securityContext);
    }

    @Test
    @DisplayName("Should continue filter chain when Authorization header doesn't start with Bearer")
    void shouldContinueFilterChainWhenAuthorizationHeaderDoesntStartWithBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(anyString());
        verifyNoInteractions(securityContext);
    }

    @Test
    @DisplayName("Should return 401 when token is invalid")
    void shouldReturn401WhenTokenIsInvalid() throws ServletException, IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn(BEARER_INVALID_TOKEN);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(jwtService.isTokenValid(INVALID_TOKEN)).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).getWriter();
        assertEquals("Invalid or expired token", stringWriter.toString());
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(securityContext);
    }

    @Test
    @DisplayName("Should return 401 when token type is not access")
    void shouldReturn401WhenTokenTypeIsNotAccess() throws ServletException, IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn(BEARER_VALID_TOKEN);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn("refresh");
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).getWriter();
        assertEquals("Access token required", stringWriter.toString());
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(securityContext);
    }

    @Test
    @DisplayName("Should return 401 when JWT processing throws exception")
    void shouldReturn401WhenJwtProcessingThrowsException() throws ServletException, IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn(BEARER_VALID_TOKEN);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(jwtService.isTokenValid(VALID_TOKEN)).thenThrow(new RuntimeException("Token parsing failed"));
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).getWriter();
        assertEquals("Invalid token format", stringWriter.toString());
        verify(filterChain, never()).doFilter(request, response);
        verifyNoInteractions(securityContext);
    }

    @Test
    @DisplayName("Should authenticate successfully with valid access token")
    void shouldAuthenticateSuccessfullyWithValidAccessToken() throws ServletException, IOException {
        Long userId = 123L;
        String email = "test@example.com";

        when(request.getHeader("Authorization")).thenReturn(BEARER_VALID_TOKEN);
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn("access");
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(userId);
        when(jwtService.extractEmail(VALID_TOKEN)).thenReturn(email);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).isTokenValid(VALID_TOKEN);
        verify(jwtService).extractTokenType(VALID_TOKEN);
        verify(jwtService).extractUserId(VALID_TOKEN);
        verify(jwtService).extractEmail(VALID_TOKEN);
        verify(securityContext).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should log security events with IP address")
    void shouldLogSecurityEventsWithIpAddress() throws ServletException, IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        String clientIp = "192.168.1.100";

        when(request.getHeader("Authorization")).thenReturn(BEARER_INVALID_TOKEN);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(jwtService.isTokenValid(INVALID_TOKEN)).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(request).getRemoteAddr();
        // Note: We can't easily verify logging calls without additional setup,
        // but we can verify that getRemoteAddr() was called for logging purposes
    }

    @Test
    @DisplayName("Should handle malformed authorization header gracefully")
    void shouldHandleMalformedAuthorizationHeaderGracefully() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Should continue with filter chain since it doesn't start properly with "Bearer "
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("Should handle empty token after Bearer prefix")
    void shouldHandleEmptyTokenAfterBearerPrefix() throws ServletException, IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(jwtService.isTokenValid("")).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(jwtService).isTokenValid("");
    }

    @Test
    @DisplayName("Should extract token correctly from Authorization header")
    void shouldExtractTokenCorrectlyFromAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(BEARER_VALID_TOKEN);
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn("access");
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(1L);
        when(jwtService.extractEmail(VALID_TOKEN)).thenReturn("test@example.com");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).isTokenValid(VALID_TOKEN);
        verify(jwtService).extractTokenType(VALID_TOKEN);
        // Verify that the token was correctly extracted by checking it was passed to JWT service
    }

    @Test
    @DisplayName("Should set proper authentication details")
    void shouldSetProperAuthenticationDetails() throws ServletException, IOException {
        Long userId = 456L;
        String email = "user@example.com";

        when(request.getHeader("Authorization")).thenReturn(BEARER_VALID_TOKEN);
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn("access");
        when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(userId);
        when(jwtService.extractEmail(VALID_TOKEN)).thenReturn(email);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(argThat(auth -> {
            if (auth.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
                return principal.userId().equals(userId) && 
                       principal.email().equals(email) &&
                       auth.getAuthorities().stream()
                           .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"));
            }
            return false;
        }));
    }

    @Test
    @DisplayName("Should handle different token types correctly")
    void shouldHandleDifferentTokenTypesCorrectly() throws ServletException, IOException {
        String[] invalidTokenTypes = {"refresh", "reset", "verify", "unknown", null};
        
        for (String tokenType : invalidTokenTypes) {
            reset(response, filterChain, jwtService);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(request.getHeader("Authorization")).thenReturn(BEARER_VALID_TOKEN);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractTokenType(VALID_TOKEN)).thenReturn(tokenType);
            when(response.getWriter()).thenReturn(printWriter);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("Should clear security context on authentication failure")
    void shouldClearSecurityContextOnAuthenticationFailure() throws ServletException, IOException {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn(BEARER_INVALID_TOKEN);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(jwtService.isTokenValid(INVALID_TOKEN)).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify that no authentication was set in the security context
        verify(securityContext, never()).setAuthentication(any());
    }
}