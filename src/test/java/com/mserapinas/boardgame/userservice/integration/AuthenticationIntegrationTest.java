package com.mserapinas.boardgame.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.LoginRequest;
import com.mserapinas.boardgame.userservice.dto.request.RefreshTokenRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthenticationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String AUTH_BASE_URL = "/api/v1/auth";

    @Test
    @DisplayName("Should complete full authentication flow successfully")
    @Transactional
    void shouldCompleteFullAuthenticationFlowSuccessfully() throws Exception {
        // Step 1: Register a new user
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "Test User", "Password123!");
        
        MvcResult registerResult = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andReturn();

        // Extract tokens from registration response
        String registerResponse = registerResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(registerResponse);
        String initialAccessToken = jsonNode.get("token").asText();
        String initialRefreshToken = jsonNode.get("refreshToken").asText();

        // Step 2: Use access token to access protected endpoint
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + initialAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));

        // Step 3: Login with the same credentials
        LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123!");
        
        MvcResult loginResult = mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andReturn();

        // Extract tokens from login response
        String loginResponse = loginResult.getResponse().getContentAsString();
        jsonNode = objectMapper.readTree(loginResponse);
        String loginAccessToken = jsonNode.get("token").asText();

        // Step 4: Use login access token to access protected endpoint
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + loginAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // Step 5: Use refresh token to get new access token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(initialRefreshToken);
        
        MvcResult refreshResult = mockMvc.perform(post(AUTH_BASE_URL + "/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andReturn();

        // Extract new tokens from refresh response
        String refreshResponse = refreshResult.getResponse().getContentAsString();
        jsonNode = objectMapper.readTree(refreshResponse);
        String newAccessToken = jsonNode.get("token").asText();

        // Step 6: Use new access token to access protected endpoint
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Should prevent duplicate user registration")
    @Transactional
    void shouldPreventDuplicateUserRegistration() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("duplicate@example.com", "Test User", "Password123!");
        
        // First registration should succeed
        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Second registration with same email should fail
        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should reject invalid login credentials")
    @Transactional
    void shouldRejectInvalidLoginCredentials() throws Exception {
        // Register a user first
        RegisterRequest registerRequest = new RegisterRequest("valid@example.com", "Test User", "Password123!");
        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try to login with wrong password
        LoginRequest wrongPasswordRequest = new LoginRequest("valid@example.com", "WrongPassword");
        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized());

        // Try to login with non-existent email
        LoginRequest nonExistentEmailRequest = new LoginRequest("nonexistent@example.com", "Password123!");
        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentEmailRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject invalid refresh token")
    @Transactional
    void shouldRejectInvalidRefreshToken() throws Exception {
        // Try to refresh with invalid token
        RefreshTokenRequest invalidRefreshRequest = new RefreshTokenRequest("invalid.refresh.token");
        
        mockMvc.perform(post(AUTH_BASE_URL + "/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRefreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject access token as refresh token")
    @Transactional
    void shouldRejectAccessTokenAsRefreshToken() throws Exception {
        // Register and get tokens
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "Test User", "Password123!");
        
        MvcResult registerResult = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract access token
        String registerResponse = registerResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(registerResponse);
        String accessToken = jsonNode.get("token").asText();

        // Try to use access token as refresh token
        RefreshTokenRequest accessTokenAsRefreshRequest = new RefreshTokenRequest(accessToken);
        
        mockMvc.perform(post(AUTH_BASE_URL + "/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accessTokenAsRefreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should enforce password validation rules")
    @Transactional
    void shouldEnforcePasswordValidationRules() throws Exception {
        String[] weakPasswords = {
            "weak",           // Too short
            "password",       // No numbers/special chars
            "12345678",       // No letters
            "PASSWORD123"     // No special chars
        };

        for (String weakPassword : weakPasswords) {
            RegisterRequest weakPasswordRequest = new RegisterRequest("test@example.com", "Test User", weakPassword);
            
            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(weakPasswordRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Should enforce email validation rules")
    @Transactional
    void shouldEnforceEmailValidationRules() throws Exception {
        String[] invalidEmails = {
            "invalid-email",
            "test@",
            "@example.com",
            "test.example.com",
            ""
        };

        for (String invalidEmail : invalidEmails) {
            RegisterRequest invalidEmailRequest = new RegisterRequest(invalidEmail, "Test User", "Password123!");
            
            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    @Transactional
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        String[] protectedEndpoints = {
            "/api/v1/users/me",
            "/api/v1/collection",
            "/api/v1/collection/games"
        };

        for (String endpoint : protectedEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Should reject expired or malformed tokens")
    @Transactional
    void shouldRejectExpiredOrMalformedTokens() throws Exception {
        String[] invalidTokens = {
            "malformed.token",
            "Bearer malformed.token",
            "",
            "expired.jwt.token.here"
        };

        for (String invalidToken : invalidTokens) {
            mockMvc.perform(get("/api/v1/users/me")
                    .header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized());
        }
    }
}