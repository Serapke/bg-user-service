package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.LoginRequest;
import com.mserapinas.boardgame.userservice.dto.request.RefreshTokenRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import com.mserapinas.boardgame.userservice.dto.response.AuthResponse;
import com.mserapinas.boardgame.userservice.exception.EmailAlreadyExistsException;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
import com.mserapinas.boardgame.userservice.model.TokenInfo;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    private AuthenticationService authenticationService;
    
    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private RefreshTokenRequest validRefreshTokenRequest;
    private User testUser;
    private TokenInfo testAccessToken;
    private TokenInfo testRefreshToken;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, jwtService, passwordEncoder);
        
        validRegisterRequest = new RegisterRequest("test@example.com", "Test User", "Password123!");
        validLoginRequest = new LoginRequest("test@example.com", "Password123!");
        validRefreshTokenRequest = new RefreshTokenRequest("valid.refresh.token");
        
        testUser = new User("test@example.com", "Test User", "hashedPassword");
        testUser.setId(1L);
        testUser.setCreatedAt(OffsetDateTime.now());
        
        testAccessToken = new TokenInfo("access.token.here", OffsetDateTime.now().plusHours(1));
        testRefreshToken = new TokenInfo("refresh.token.here", OffsetDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("Should signup new user successfully")
    void shouldSignupNewUserSuccessfully() {
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser.getId(), testUser.getEmail())).thenReturn(testAccessToken);
        when(jwtService.generateRefreshToken(testUser.getId())).thenReturn(testRefreshToken);
        
        AuthResponse result = authenticationService.signup(validRegisterRequest);
        
        assertNotNull(result);
        assertEquals(testAccessToken.token(), result.token());
        assertEquals(testRefreshToken.token(), result.refreshToken());
        assertEquals(testAccessToken.expiresAt(), result.expiresAt());
        assertEquals(testRefreshToken.expiresAt(), result.refreshExpiresAt());
        assertNotNull(result.user());
        assertEquals(testUser.getName(), result.user().name());
        assertEquals(testUser.getName(), result.user().name());
        
        verify(userRepository).existsByEmail(validRegisterRequest.email());
        verify(passwordEncoder).encode(validRegisterRequest.password());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateAccessToken(testUser.getId(), testUser.getEmail());
        verify(jwtService).generateRefreshToken(testUser.getId());
    }

    @Test
    @DisplayName("Should throw exception when email already exists during signup")
    void shouldThrowExceptionWhenEmailAlreadyExistsDuringSignup() {
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(true);
        
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
            () -> authenticationService.signup(validRegisterRequest));
        
        assertTrue(exception.getMessage().contains(validRegisterRequest.email()));
        
        verify(userRepository).existsByEmail(validRegisterRequest.email());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully with valid credentials")
    void shouldLoginUserSuccessfullyWithValidCredentials() {
        when(userRepository.findByEmail(validLoginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(validLoginRequest.password(), testUser.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(testUser.getId(), testUser.getEmail())).thenReturn(testAccessToken);
        when(jwtService.generateRefreshToken(testUser.getId())).thenReturn(testRefreshToken);
        
        AuthResponse result = authenticationService.login(validLoginRequest);
        
        assertNotNull(result);
        assertEquals(testAccessToken.token(), result.token());
        assertEquals(testRefreshToken.token(), result.refreshToken());
        assertEquals(testAccessToken.expiresAt(), result.expiresAt());
        assertEquals(testRefreshToken.expiresAt(), result.refreshExpiresAt());
        assertNotNull(result.user());
        assertEquals(testUser.getName(), result.user().name());
        
        verify(userRepository).findByEmail(validLoginRequest.email());
        verify(passwordEncoder).matches(validLoginRequest.password(), testUser.getPassword());
        verify(jwtService).generateAccessToken(testUser.getId(), testUser.getEmail());
        verify(jwtService).generateRefreshToken(testUser.getId());
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void shouldThrowExceptionWhenUserNotFoundDuringLogin() {
        when(userRepository.findByEmail(validLoginRequest.email())).thenReturn(Optional.empty());
        
        assertThrows(InvalidCredentialsException.class,
            () -> authenticationService.login(validLoginRequest));
        
        verify(userRepository).findByEmail(validLoginRequest.email());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect during login")
    void shouldThrowExceptionWhenPasswordIsIncorrectDuringLogin() {
        when(userRepository.findByEmail(validLoginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(validLoginRequest.password(), testUser.getPassword())).thenReturn(false);
        
        assertThrows(InvalidCredentialsException.class,
            () -> authenticationService.login(validLoginRequest));
        
        verify(userRepository).findByEmail(validLoginRequest.email());
        verify(passwordEncoder).matches(validLoginRequest.password(), testUser.getPassword());
        verify(jwtService, never()).generateAccessToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void shouldRefreshTokenSuccessfullyWithValidRefreshToken() {
        Long userId = 1L;
        String refreshTokenString = validRefreshTokenRequest.refreshToken();
        
        when(jwtService.isTokenValid(refreshTokenString)).thenReturn(true);
        when(jwtService.extractTokenType(refreshTokenString)).thenReturn("refresh");
        when(jwtService.extractUserId(refreshTokenString)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser.getId(), testUser.getEmail())).thenReturn(testAccessToken);
        when(jwtService.generateRefreshToken(testUser.getId())).thenReturn(testRefreshToken);
        
        AuthResponse result = authenticationService.refreshToken(validRefreshTokenRequest);
        
        assertNotNull(result);
        assertEquals(testAccessToken.token(), result.token());
        assertEquals(testRefreshToken.token(), result.refreshToken());
        assertEquals(testAccessToken.expiresAt(), result.expiresAt());
        assertEquals(testRefreshToken.expiresAt(), result.refreshExpiresAt());
        assertNotNull(result.user());
        
        verify(jwtService).isTokenValid(refreshTokenString);
        verify(jwtService).extractTokenType(refreshTokenString);
        verify(jwtService).extractUserId(refreshTokenString);
        verify(userRepository).findById(userId);
        verify(jwtService).generateAccessToken(testUser.getId(), testUser.getEmail());
        verify(jwtService).generateRefreshToken(testUser.getId());
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        String refreshTokenString = validRefreshTokenRequest.refreshToken();
        
        when(jwtService.isTokenValid(refreshTokenString)).thenReturn(false);
        
        assertThrows(InvalidCredentialsException.class,
            () -> authenticationService.refreshToken(validRefreshTokenRequest));
        
        verify(jwtService).isTokenValid(refreshTokenString);
        verify(jwtService, never()).extractTokenType(anyString());
        verify(jwtService, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("Should throw exception when token type is not refresh")
    void shouldThrowExceptionWhenTokenTypeIsNotRefresh() {
        String refreshTokenString = validRefreshTokenRequest.refreshToken();
        
        when(jwtService.isTokenValid(refreshTokenString)).thenReturn(true);
        when(jwtService.extractTokenType(refreshTokenString)).thenReturn("access");
        
        assertThrows(InvalidCredentialsException.class,
            () -> authenticationService.refreshToken(validRefreshTokenRequest));
        
        verify(jwtService).isTokenValid(refreshTokenString);
        verify(jwtService).extractTokenType(refreshTokenString);
        verify(jwtService, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("Should throw exception when user not found for refresh token")
    void shouldThrowExceptionWhenUserNotFoundForRefreshToken() {
        Long userId = 1L;
        String refreshTokenString = validRefreshTokenRequest.refreshToken();
        
        when(jwtService.isTokenValid(refreshTokenString)).thenReturn(true);
        when(jwtService.extractTokenType(refreshTokenString)).thenReturn("refresh");
        when(jwtService.extractUserId(refreshTokenString)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThrows(InvalidCredentialsException.class,
            () -> authenticationService.refreshToken(validRefreshTokenRequest));
        
        verify(jwtService).isTokenValid(refreshTokenString);
        verify(jwtService).extractTokenType(refreshTokenString);
        verify(jwtService).extractUserId(refreshTokenString);
        verify(userRepository).findById(userId);
        verify(jwtService, never()).generateAccessToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should hash password before saving during signup")
    void shouldHashPasswordBeforeSavingDuringSignup() {
        String plainPassword = validRegisterRequest.password();
        String hashedPassword = "hashedPassword123";
        
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(plainPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(hashedPassword, savedUser.getPassword());
            return testUser;
        });
        when(jwtService.generateAccessToken(anyLong(), anyString())).thenReturn(testAccessToken);
        when(jwtService.generateRefreshToken(anyLong())).thenReturn(testRefreshToken);
        
        authenticationService.signup(validRegisterRequest);
        
        verify(passwordEncoder).encode(plainPassword);
    }

    @Test
    @DisplayName("Should set creation timestamp during signup")
    void shouldSetCreationTimestampDuringSignup() {
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertNotNull(savedUser.getCreatedAt());
            assertTrue(savedUser.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(1)));
            return testUser;
        });
        when(jwtService.generateAccessToken(anyLong(), anyString())).thenReturn(testAccessToken);
        when(jwtService.generateRefreshToken(anyLong())).thenReturn(testRefreshToken);
        
        authenticationService.signup(validRegisterRequest);
        
        verify(userRepository).save(any(User.class));
    }
}