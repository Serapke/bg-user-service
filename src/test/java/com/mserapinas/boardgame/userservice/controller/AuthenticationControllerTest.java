package com.mserapinas.boardgame.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.LoginRequest;
import com.mserapinas.boardgame.userservice.dto.request.RefreshTokenRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import com.mserapinas.boardgame.userservice.dto.response.AuthResponse;
import com.mserapinas.boardgame.userservice.dto.response.UserResponse;
import com.mserapinas.boardgame.userservice.exception.EmailAlreadyExistsException;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import com.mserapinas.boardgame.userservice.service.AuthenticationService;
import com.mserapinas.boardgame.userservice.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private static final String BASE_URL = "/api/v1/auth";

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "Test User", "Password123!");
        
        UserResponse userResponse = new UserResponse(1L, "Test User");
        AuthResponse authResponse = new AuthResponse(
            "access.token", "refresh.token", 
            OffsetDateTime.now().plusHours(1), OffsetDateTime.now().plusDays(7),
            userResponse
        );
        
        when(authenticationService.signup(any(RegisterRequest.class))).thenReturn(authResponse);
        
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("access.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.refreshExpiresAt").exists())
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.name").value("Test User"));
        
        verify(authenticationService).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when registering with invalid email")
    void shouldReturnBadRequestWhenRegisteringWithInvalidEmail() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("invalid-email", "Test User", "Password123!");
        
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when registering with weak password")
    void shouldReturnBadRequestWhenRegisteringWithWeakPassword() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("test@example.com", "Test User", "weak");
        
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when registering with empty name")
    void shouldReturnBadRequestWhenRegisteringWithEmptyName() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("test@example.com", "", "Password123!");
        
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return conflict when email already exists")
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("existing@example.com", "Test User", "Password123!");
        
        when(authenticationService.signup(any(RegisterRequest.class)))
            .thenThrow(new EmailAlreadyExistsException("existing@example.com"));
        
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        
        verify(authenticationService).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginUserSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        
        UserResponse userResponse = new UserResponse(1L, "Test User");
        AuthResponse authResponse = new AuthResponse(
            "access.token", "refresh.token",
            OffsetDateTime.now().plusHours(1), OffsetDateTime.now().plusDays(7),
            userResponse
        );
        
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(authResponse);
        
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("access.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token"))
                .andExpect(jsonPath("$.user.name").value("Test User"));
        
        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return unauthorized when login credentials are invalid")
    void shouldReturnUnauthorizedWhenLoginCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword");
        
        when(authenticationService.login(any(LoginRequest.class)))
            .thenThrow(new InvalidCredentialsException());
        
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when login with invalid email format")
    void shouldReturnBadRequestWhenLoginWithInvalidEmailFormat() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("not-an-email", "Password123!");
        
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when login with empty password")
    void shouldReturnBadRequestWhenLoginWithEmptyPassword() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("test@example.com", "");
        
        mockMvc.perform(post(BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid.refresh.token");
        
        UserResponse userResponse = new UserResponse(1L, "Test User");
        AuthResponse authResponse = new AuthResponse(
            "new.access.token", "new.refresh.token",
            OffsetDateTime.now().plusHours(1), OffsetDateTime.now().plusDays(7),
            userResponse
        );
        
        when(authenticationService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);
        
        mockMvc.perform(post(BASE_URL + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"))
                .andExpect(jsonPath("$.user.name").value("Test User"));
        
        verify(authenticationService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("Should return unauthorized when refresh token is invalid")
    void shouldReturnUnauthorizedWhenRefreshTokenIsInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid.refresh.token");
        
        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
            .thenThrow(new InvalidCredentialsException());
        
        mockMvc.perform(post(BASE_URL + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        verify(authenticationService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when refresh token is empty")
    void shouldReturnBadRequestWhenRefreshTokenIsEmpty() throws Exception {
        RefreshTokenRequest invalidRequest = new RefreshTokenRequest("");
        
        mockMvc.perform(post(BASE_URL + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when refresh token is null")
    void shouldReturnBadRequestWhenRefreshTokenIsNull() throws Exception {
        RefreshTokenRequest invalidRequest = new RefreshTokenRequest(null);
        
        mockMvc.perform(post(BASE_URL + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void shouldHandleMalformedJsonGracefully() throws Exception {
        String malformedJson = "{\"email\":\"test@example.com\",\"name\":}"; // Invalid JSON
        
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
        
        verify(authenticationService, never()).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should require Content-Type header")
    void shouldRequireContentTypeHeader() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "Test User", "Password123!");
        
        mockMvc.perform(post(BASE_URL + "/register")
                .content(objectMapper.writeValueAsString(request))) // Missing Content-Type
                .andExpect(status().isUnsupportedMediaType());
        
        verify(authenticationService, never()).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should accept valid email formats")
    void shouldAcceptValidEmailFormats() throws Exception {
        String[] validEmails = {
            "test@example.com",
            "user.name@example.co.uk", 
            "user+tag@example.org",
            "123@example.com"
        };
        
        UserResponse userResponse = new UserResponse(1L, "Test User");
        AuthResponse authResponse = new AuthResponse(
            "access.token", "refresh.token",
            OffsetDateTime.now().plusHours(1), OffsetDateTime.now().plusDays(7),
            userResponse
        );
        
        when(authenticationService.signup(any(RegisterRequest.class))).thenReturn(authResponse);
        
        for (String email : validEmails) {
            RegisterRequest request = new RegisterRequest(email, "Test User", "Password123!");
            
            mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        verify(authenticationService, times(validEmails.length)).signup(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should accept valid strong passwords")
    void shouldAcceptValidStrongPasswords() throws Exception {
        String[] validPasswords = {
            "Password123!",
            "MySecure@Pass1",
            "ComplexP@ss123",
            "StrongPassword2024!"
        };
        
        UserResponse userResponse = new UserResponse(1L, "Test User");
        AuthResponse authResponse = new AuthResponse(
            "access.token", "refresh.token",
            OffsetDateTime.now().plusHours(1), OffsetDateTime.now().plusDays(7),
            userResponse
        );
        
        when(authenticationService.signup(any(RegisterRequest.class))).thenReturn(authResponse);
        
        for (String password : validPasswords) {
            RegisterRequest request = new RegisterRequest("test@example.com", "Test User", password);
            
            mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        
        verify(authenticationService, times(validPasswords.length)).signup(any(RegisterRequest.class));
    }
}