package com.mserapinas.boardgame.userservice.security;

import com.mserapinas.boardgame.userservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authorizationHeader.substring(7);
        
        try {
            if (!jwtService.isTokenValid(token)) {
                logger.warn("Invalid JWT token provided from IP: {}", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
            
            String tokenType = jwtService.extractTokenType(token);
            if (!"access".equals(tokenType)) {
                logger.warn("Wrong token type '{}' provided from IP: {}", tokenType, request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Access token required");
                return;
            }
        } catch (Exception e) {
            logger.warn("JWT token processing failed from IP: {} - {}", request.getRemoteAddr(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token format");
            return;
        }
        
        Long userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);
        
        UserPrincipal userPrincipal = new UserPrincipal(userId, email);
        
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userPrincipal, 
            null, 
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        
        filterChain.doFilter(request, response);
    }
}