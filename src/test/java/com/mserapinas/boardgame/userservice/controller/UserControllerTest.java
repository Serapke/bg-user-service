package com.mserapinas.boardgame.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.UpdateUserProfileRequest;
import com.mserapinas.boardgame.userservice.exception.InvalidCredentialsException;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/users";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final Long TEST_USER_ID = 1L;

    @Test
    @DisplayName("Should get current user profile successfully")
    void shouldGetCurrentUserProfileSuccessfully() throws Exception {
        User testUser = new User("test@example.com", "Test User", "hashedPassword");
        testUser.setId(TEST_USER_ID);
        testUser.setCreatedAt(OffsetDateTime.now());

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(userRepository).findById(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return unauthorized when getting profile without X-User-ID header")
    void shouldReturnUnauthorizedWhenGettingProfileWithoutHeader() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isBadRequest()); // Missing required header
    }

    @Test
    @DisplayName("Should return unauthorized when user not found")
    void shouldReturnUnauthorizedWhenUserNotFound() throws Exception {
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isUnauthorized());

        verify(userRepository).findById(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should update current user profile successfully")
    void shouldUpdateCurrentUserProfileSuccessfully() throws Exception {
        String newName = "Updated Name";
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(newName);

        User updatedUser = new User("test@example.com", newName, "hashedPassword");
        updatedUser.setId(TEST_USER_ID);
        updatedUser.setCreatedAt(OffsetDateTime.now());

        when(userService.updateUserProfile(eq(TEST_USER_ID), any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(updatedUser.getId()))
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(userService).updateUserProfile(eq(TEST_USER_ID), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when updating profile with invalid data")
    void shouldReturnBadRequestWhenUpdatingProfileWithInvalidData() throws Exception {
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest(null); // name is required

        mockMvc.perform(put(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserProfile(any(Long.class), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when updating profile with empty name")
    void shouldReturnBadRequestWhenUpdatingProfileWithEmptyName() throws Exception {
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest(""); // empty name

        mockMvc.perform(put(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserProfile(any(Long.class), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return unauthorized when updating profile without X-User-ID header")
    void shouldReturnUnauthorizedWhenUpdatingProfileWithoutHeader() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("New Name");

        mockMvc.perform(put(BASE_URL + "/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Missing required header

        verify(userService, never()).updateUserProfile(any(Long.class), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should return unauthorized when updating profile for non-existent user")
    void shouldReturnUnauthorizedWhenUpdatingProfileForNonExistentUser() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Valid Name");

        when(userService.updateUserProfile(eq(TEST_USER_ID), any(UpdateUserProfileRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(put(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userService).updateUserProfile(eq(TEST_USER_ID), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should delete current user account successfully")
    void shouldDeleteCurrentUserAccountSuccessfully() throws Exception {
        doNothing().when(userService).deleteUserAccount(TEST_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNoContent());

        verify(userService).deleteUserAccount(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return bad request when deleting account without X-User-ID header")
    void shouldReturnBadRequestWhenDeletingAccountWithoutHeader() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/me"))
                .andExpect(status().isBadRequest()); // Missing required header

        verify(userService, never()).deleteUserAccount(any(Long.class));
    }

    @Test
    @DisplayName("Should return unauthorized when deleting non-existent user account")
    void shouldReturnUnauthorizedWhenDeletingNonExistentUserAccount() throws Exception {
        doThrow(new InvalidCredentialsException()).when(userService).deleteUserAccount(TEST_USER_ID);

        mockMvc.perform(delete(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isUnauthorized());

        verify(userService).deleteUserAccount(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should accept valid name with special characters")
    void shouldAcceptValidNameWithSpecialCharacters() throws Exception {
        String nameWithSpecialChars = "José María O'Connor-Smith";
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(nameWithSpecialChars);

        User updatedUser = new User("test@example.com", nameWithSpecialChars, "hashedPassword");
        updatedUser.setId(TEST_USER_ID);
        updatedUser.setCreatedAt(OffsetDateTime.now());

        when(userService.updateUserProfile(eq(TEST_USER_ID), any(UpdateUserProfileRequest.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(nameWithSpecialChars));

        verify(userService).updateUserProfile(eq(TEST_USER_ID), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should handle malformed JSON request")
    void shouldHandleMalformedJsonRequest() throws Exception {
        String malformedJson = "{\"name\":}"; // Invalid JSON

        mockMvc.perform(put(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUserProfile(any(Long.class), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should require content-type header for update request")
    void shouldRequireContentTypeHeaderForUpdateRequest() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("Valid Name");

        mockMvc.perform(put(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID)
                .content(objectMapper.writeValueAsString(request))) // Missing Content-Type header
                .andExpect(status().isUnsupportedMediaType());

        verify(userService, never()).updateUserProfile(any(Long.class), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("Should handle invalid user ID format in header")
    void shouldHandleInvalidUserIdFormatInHeader() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me")
                .header(USER_ID_HEADER, "invalid-id"))
                .andExpect(status().isBadRequest()); // Invalid user ID format
    }
}