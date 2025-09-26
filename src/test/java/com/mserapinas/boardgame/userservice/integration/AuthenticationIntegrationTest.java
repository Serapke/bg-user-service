package com.mserapinas.boardgame.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.LoginRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @DisplayName("Should register a new user successfully")
    @Transactional
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "Test User", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    @DisplayName("Should login with valid credentials")
    @Transactional
    void shouldLoginWithValidCredentials() throws Exception {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest("login@example.com", "Login User", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login with the same credentials
        LoginRequest loginRequest = new LoginRequest("login@example.com", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Login User"));
    }

    @Test
    @DisplayName("Should return conflict when registering with existing email")
    @Transactional
    void shouldReturnConflictWhenRegisteringWithExistingEmail() throws Exception {
        RegisterRequest firstRequest = new RegisterRequest("duplicate@example.com", "First User", "Password123!");

        // Register first user
        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Try to register second user with same email
        RegisterRequest duplicateRequest = new RegisterRequest("duplicate@example.com", "Second User", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return unauthorized for invalid login credentials")
    @Transactional
    void shouldReturnUnauthorizedForInvalidLoginCredentials() throws Exception {
        // Register a user first
        RegisterRequest registerRequest = new RegisterRequest("valid@example.com", "Valid User", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try to login with wrong password
        LoginRequest invalidLoginRequest = new LoginRequest("valid@example.com", "WrongPassword!");

        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return unauthorized for non-existent user login")
    @Transactional
    void shouldReturnUnauthorizedForNonExistentUserLogin() throws Exception {
        LoginRequest nonExistentUserRequest = new LoginRequest("nonexistent@example.com", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUserRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle special characters in user names during registration")
    @Transactional
    void shouldHandleSpecialCharactersInUserNamesDuringRegistration() throws Exception {
        RegisterRequest specialNameRequest = new RegisterRequest("special@example.com", "José María O'Connor-Smith", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialNameRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("José María O'Connor-Smith"));
    }

    @Test
    @DisplayName("Should handle various valid email formats during registration")
    @Transactional
    void shouldHandleVariousValidEmailFormatsDuringRegistration() throws Exception {
        String[] validEmails = {
            "user@example.com",
            "user.name@example.co.uk",
            "user+tag@example.org",
            "123@example.com"
        };

        for (int i = 0; i < validEmails.length; i++) {
            RegisterRequest request = new RegisterRequest(validEmails[i], "User " + i, "Password123!");

            mockMvc.perform(post(AUTH_BASE_URL + "/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("User " + i));
        }
    }

    @Test
    @DisplayName("Should return bad request for invalid email format")
    @Transactional
    void shouldReturnBadRequestForInvalidEmailFormat() throws Exception {
        RegisterRequest invalidEmailRequest = new RegisterRequest("invalid-email", "Test User", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request for weak password")
    @Transactional
    void shouldReturnBadRequestForWeakPassword() throws Exception {
        RegisterRequest weakPasswordRequest = new RegisterRequest("weak@example.com", "Test User", "weak");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weakPasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request for empty name")
    @Transactional
    void shouldReturnBadRequestForEmptyName() throws Exception {
        RegisterRequest emptyNameRequest = new RegisterRequest("empty@example.com", "", "Password123!");

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyNameRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request for malformed JSON")
    @Transactional
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        String malformedJson = "{\"email\":\"test@example.com\",\"name\":}"; // Invalid JSON

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle concurrent user registrations with different emails")
    @Transactional
    void shouldHandleConcurrentUserRegistrationsWithDifferentEmails() throws Exception {
        // Simulate concurrent registrations by performing multiple requests quickly
        RegisterRequest user1 = new RegisterRequest("concurrent1@example.com", "Concurrent User 1", "Password123!");
        RegisterRequest user2 = new RegisterRequest("concurrent2@example.com", "Concurrent User 2", "Password123!");

        // Both should succeed since they have different emails
        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Concurrent User 1"));

        mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Concurrent User 2"));
    }
}