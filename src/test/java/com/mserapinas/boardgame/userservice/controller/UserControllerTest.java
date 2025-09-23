package com.mserapinas.boardgame.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.UpdateUserProfileRequest;
import com.mserapinas.boardgame.userservice.dto.response.UserProfileDto;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import com.mserapinas.boardgame.userservice.service.JwtService;
import com.mserapinas.boardgame.userservice.service.UserContextService;
import com.mserapinas.boardgame.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserContextService userContextService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/users";

    @Test
    @DisplayName("Should get current user profile successfully")
    void shouldGetCurrentUserProfileSuccessfully() throws Exception {
        User testUser = new User("test@example.com", "Test User", "hashedPassword");
        testUser.setId(1L);
        testUser.setCreatedAt(OffsetDateTime.now());
        
        UserProfileDto expectedProfile = new UserProfileDto(
            testUser.getId(),
            testUser.getEmail(),
            testUser.getName(),
            testUser.getCreatedAt()
        );
        
        when(userContextService.getCurrentUser()).thenReturn(Optional.of(testUser));
        
        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isOk());
        
        verify(userContextService).getCurrentUser();
    }

    @Test
    @DisplayName("Should return unauthorized when getting profile without authentication")
    void shouldReturnUnauthorizedWhenGettingProfileWithoutAuthentication() throws Exception {
        when(userContextService.getCurrentUser()).thenReturn(Optional.empty());
        
        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isUnauthorized());
        
        verify(userContextService).getCurrentUser();
    }

    @Test
    @DisplayName("Should return unauthorized when no current user found")
    void shouldReturnUnauthorizedWhenNoCurrentUserFound() throws Exception {
        when(userContextService.getCurrentUser()).thenReturn(Optional.empty());
        
        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isUnauthorized());
        
        verify(userContextService).getCurrentUser();
    }

    @Test
    @DisplayName("Should update current user profile successfully")
    void shouldUpdateCurrentUserProfileSuccessfully() throws Exception {
        String newName = "Updated Name";
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(newName);
        
        User updatedUser = new User("test@example.com", newName, "hashedPassword");
        updatedUser.setId(1L);
        updatedUser.setCreatedAt(OffsetDateTime.now());
        
        when(userService.updateCurrentUserProfile(any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedUser);
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(updatedUser.getId()))
                .andExpect(jsonPath("$.email").value(updatedUser.getEmail()))
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.createdAt").exists());
        
        verify(userService).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when updating profile with invalid data")
    void shouldReturnBadRequestWhenUpdatingProfileWithInvalidData() throws Exception {
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest(null); // name is required
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when updating profile with empty name")
    void shouldReturnBadRequestWhenUpdatingProfileWithEmptyName() throws Exception {
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest(""); // empty name
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when updating profile with name too long")
    void shouldReturnBadRequestWhenUpdatingProfileWithNameTooLong() throws Exception {
        String longName = "a".repeat(256); // Assuming there's a length limit
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest(longName);
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return unauthorized when updating profile without authentication")
    void shouldReturnUnauthorizedWhenUpdatingProfileWithoutAuthentication() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("New Name");
        
        when(userService.updateCurrentUserProfile(any(UpdateUserProfileRequest.class)))
            .thenThrow(new InvalidCredentialsException());
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        verify(userService).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should handle service exception when updating profile")
    void shouldHandleServiceExceptionWhenUpdatingProfile() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Valid Name");
        
        when(userService.updateCurrentUserProfile(any(UpdateUserProfileRequest.class)))
                .thenThrow(new InvalidCredentialsException());
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        
        verify(userService).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should delete current user account successfully")
    void shouldDeleteCurrentUserAccountSuccessfully() throws Exception {
        doNothing().when(userService).deleteCurrentUserAccount();
        
        mockMvc.perform(delete(BASE_URL + "/me"))
                .andExpect(status().isNoContent());
        
        verify(userService).deleteCurrentUserAccount();
    }

    @Test
    @DisplayName("Should return unauthorized when deleting account without authentication")
    void shouldReturnUnauthorizedWhenDeletingAccountWithoutAuthentication() throws Exception {
        doThrow(new InvalidCredentialsException()).when(userService).deleteCurrentUserAccount();
        
        mockMvc.perform(delete(BASE_URL + "/me"))
                .andExpect(status().isUnauthorized());
        
        verify(userService).deleteCurrentUserAccount();
    }

    @Test
    @DisplayName("Should handle service exception when deleting account")
    void shouldHandleServiceExceptionWhenDeletingAccount() throws Exception {
        doThrow(new InvalidCredentialsException()).when(userService).deleteCurrentUserAccount();
        
        mockMvc.perform(delete(BASE_URL + "/me"))
                .andExpect(status().isUnauthorized());
        
        verify(userService).deleteCurrentUserAccount();
    }

    @Test
    @DisplayName("Should accept valid name with special characters")
    void shouldAcceptValidNameWithSpecialCharacters() throws Exception {
        String nameWithSpecialChars = "José María O'Connor-Smith";
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(nameWithSpecialChars);
        
        User updatedUser = new User("test@example.com", nameWithSpecialChars, "hashedPassword");
        updatedUser.setId(1L);
        updatedUser.setCreatedAt(OffsetDateTime.now());
        
        when(userService.updateCurrentUserProfile(any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedUser);
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(nameWithSpecialChars));
        
        verify(userService).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should handle malformed JSON request")
    void shouldHandleMalformedJsonRequest() throws Exception {
        String malformedJson = "{\"name\":}"; // Invalid JSON
        
        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
        
        verify(userService, never()).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should require content-type header for update request")
    void shouldRequireContentTypeHeaderForUpdateRequest() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Valid Name");
        
        mockMvc.perform(put(BASE_URL + "/me")
                .content(objectMapper.writeValueAsString(request))) // Missing Content-Type header
                .andExpect(status().isUnsupportedMediaType());
        
        verify(userService, never()).updateCurrentUserProfile(any(UpdateUserProfileRequest.class));
    }
}