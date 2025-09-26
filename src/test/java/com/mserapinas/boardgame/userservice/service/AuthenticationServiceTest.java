package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.LoginRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import com.mserapinas.boardgame.userservice.dto.response.UserResponse;
import com.mserapinas.boardgame.userservice.exception.EmailAlreadyExistsException;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    private User testUser;
    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, passwordEncoder);

        testUser = new User("test@example.com", "Test User", "hashedPassword123");
        testUser.setId(1L);
        testUser.setCreatedAt(OffsetDateTime.now());

        validRegisterRequest = new RegisterRequest("test@example.com", "Test User", "Password123!");
        validLoginRequest = new LoginRequest("test@example.com", "Password123!");
    }

    @Test
    @DisplayName("Should signup user successfully")
    void shouldSignupUserSuccessfully() {
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.password())).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = authenticationService.signup(validRegisterRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Test User", result.name());

        verify(userRepository).existsByEmail(validRegisterRequest.email());
        verify(passwordEncoder).encode(validRegisterRequest.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists during signup")
    void shouldThrowExceptionWhenEmailAlreadyExistsDuringSignup() {
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
            () -> authenticationService.signup(validRegisterRequest));

        verify(userRepository).existsByEmail(validRegisterRequest.email());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginUserSuccessfully() {
        when(userRepository.findByEmail(validLoginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(validLoginRequest.password(), testUser.getPassword())).thenReturn(true);

        UserResponse result = authenticationService.login(validLoginRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Test User", result.name());

        verify(userRepository).findByEmail(validLoginRequest.email());
        verify(passwordEncoder).matches(validLoginRequest.password(), testUser.getPassword());
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void shouldThrowExceptionWhenUserNotFoundDuringLogin() {
        when(userRepository.findByEmail(validLoginRequest.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
            () -> authenticationService.login(validLoginRequest));

        verify(userRepository).findByEmail(validLoginRequest.email());
        verify(passwordEncoder, never()).matches(any(), any());
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
    }

    @Test
    @DisplayName("Should handle signup with different valid email formats")
    void shouldHandleSignupWithDifferentValidEmailFormats() {
        String[] validEmails = {
            "user@example.com",
            "user.name@example.co.uk",
            "user+tag@example.org",
            "123@example.com"
        };

        for (String email : validEmails) {
            RegisterRequest request = new RegisterRequest(email, "Test User", "Password123!");
            User user = new User(email, "Test User", "hashedPassword123");
            user.setId(1L);

            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword123");
            when(userRepository.save(any(User.class))).thenReturn(user);

            UserResponse result = authenticationService.signup(request);

            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("Test User", result.name());
        }
    }

    @Test
    @DisplayName("Should handle signup with different valid names")
    void shouldHandleSignupWithDifferentValidNames() {
        String[] validNames = {
            "John Doe",
            "José María O'Connor-Smith",
            "李小龙",
            "User 123"
        };

        for (String name : validNames) {
            RegisterRequest request = new RegisterRequest("test@example.com", name, "Password123!");
            User user = new User("test@example.com", name, "hashedPassword123");
            user.setId(1L);

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword123");
            when(userRepository.save(any(User.class))).thenReturn(user);

            UserResponse result = authenticationService.signup(request);

            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals(name, result.name());
        }
    }

    @Test
    @DisplayName("Should handle login with different password formats")
    void shouldHandleLoginWithDifferentPasswordFormats() {
        String[] validPasswords = {
            "Password123!",
            "MySecure@Pass1",
            "ComplexP@ss123",
            "StrongPassword2024!"
        };

        for (String password : validPasswords) {
            LoginRequest request = new LoginRequest("test@example.com", password);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);

            UserResponse result = authenticationService.login(request);

            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("Test User", result.name());
        }
    }

    @Test
    @DisplayName("Should set creation timestamp during signup")
    void shouldSetCreationTimestampDuringSignup() {
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.password())).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertNotNull(savedUser.getCreatedAt());
            savedUser.setId(1L);
            return savedUser;
        });

        UserResponse result = authenticationService.signup(validRegisterRequest);

        assertNotNull(result);
        verify(userRepository).save(argThat(user -> user.getCreatedAt() != null));
    }

    @Test
    @DisplayName("Should create user with correct email and name during signup")
    void shouldCreateUserWithCorrectEmailAndNameDuringSignup() {
        when(userRepository.existsByEmail(validRegisterRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(validRegisterRequest.password())).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(validRegisterRequest.email(), savedUser.getEmail());
            assertEquals(validRegisterRequest.name(), savedUser.getName());
            assertEquals("hashedPassword123", savedUser.getPassword());
            savedUser.setId(1L);
            return savedUser;
        });

        UserResponse result = authenticationService.signup(validRegisterRequest);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Test User", result.name());

        verify(userRepository).save(argThat(user ->
            user.getEmail().equals(validRegisterRequest.email()) &&
            user.getName().equals(validRegisterRequest.name()) &&
            user.getPassword().equals("hashedPassword123")
        ));
    }
}