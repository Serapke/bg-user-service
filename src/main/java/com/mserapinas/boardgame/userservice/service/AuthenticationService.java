package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.LoginRequest;
import com.mserapinas.boardgame.userservice.dto.request.RefreshTokenRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import com.mserapinas.boardgame.userservice.dto.response.AuthResponse;
import com.mserapinas.boardgame.userservice.dto.response.UserResponse;
import com.mserapinas.boardgame.userservice.exception.EmailAlreadyExistsException;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
import com.mserapinas.boardgame.userservice.model.TokenInfo;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse signup(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // Hash the password before storing
        String hashedPassword = passwordEncoder.encode(request.password());
        
        User user = new User(request.email(), request.name(), hashedPassword);
        user.setCreatedAt(OffsetDateTime.now());
        User savedUser = userRepository.save(user);

        TokenInfo accessTokenInfo = jwtService.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        TokenInfo refreshTokenInfo = jwtService.generateRefreshToken(savedUser.getId());
        UserResponse userResponse = UserResponse.from(savedUser);

        return new AuthResponse(
            accessTokenInfo.token(), 
            refreshTokenInfo.token(), 
            accessTokenInfo.expiresAt(), 
            refreshTokenInfo.expiresAt(), 
            userResponse
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);
        
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        
        TokenInfo accessTokenInfo = jwtService.generateAccessToken(user.getId(), user.getEmail());
        TokenInfo refreshTokenInfo = jwtService.generateRefreshToken(user.getId());
        UserResponse userResponse = UserResponse.from(user);
        
        return new AuthResponse(
            accessTokenInfo.token(), 
            refreshTokenInfo.token(), 
            accessTokenInfo.expiresAt(), 
            refreshTokenInfo.expiresAt(), 
            userResponse
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.isTokenValid(request.refreshToken())) {
            throw new InvalidCredentialsException();
        }
        
        String tokenType = jwtService.extractTokenType(request.refreshToken());
        if (!"refresh".equals(tokenType)) {
            throw new InvalidCredentialsException();
        }
        
        Long userId = jwtService.extractUserId(request.refreshToken());
        User user = userRepository.findById(userId)
            .orElseThrow(InvalidCredentialsException::new);
        
        TokenInfo accessTokenInfo = jwtService.generateAccessToken(user.getId(), user.getEmail());
        TokenInfo refreshTokenInfo = jwtService.generateRefreshToken(user.getId());
        UserResponse userResponse = UserResponse.from(user);
        
        return new AuthResponse(
            accessTokenInfo.token(), 
            refreshTokenInfo.token(), 
            accessTokenInfo.expiresAt(), 
            refreshTokenInfo.expiresAt(), 
            userResponse
        );
    }
}
