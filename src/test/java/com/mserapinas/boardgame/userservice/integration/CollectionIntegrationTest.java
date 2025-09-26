package com.mserapinas.boardgame.userservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.AddGameToCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateGameCollectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CollectionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String COLLECTION_BASE_URL = "/api/v1/collections";
    private static final String AUTH_BASE_URL = "/api/v1/auth";
    private static final String USER_ID_HEADER = "X-User-ID";

    private Long userId;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        // Register a user and get user ID
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "Test User", "Password123!");

        MvcResult result = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        userId = response.get("id").asLong();
    }

    @Test
    @DisplayName("Should get empty collection for new user")
    @Transactional
    void shouldGetEmptyCollectionForNewUser() throws Exception {
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header(USER_ID_HEADER, userId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games").isEmpty());
    }

    @Test
    @DisplayName("Should add game to collection successfully")
    @Transactional
    void shouldAddGameToCollectionSuccessfully() throws Exception {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1001, "Great strategy game", Set.of("Strategy", "Euro")
        );

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(1001))
                .andExpect(jsonPath("$.notes").value("Great strategy game"))
                .andExpect(jsonPath("$.labels").isArray());
    }

    @Test
    @DisplayName("Should get collection with added games")
    @Transactional
    void shouldGetCollectionWithAddedGames() throws Exception {
        // Add a game first
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1002, "Another great game", Set.of("Strategy")
        );

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get collection
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header(USER_ID_HEADER, userId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games").isNotEmpty())
                .andExpect(jsonPath("$.games[0].gameId").value(1002))
                .andExpect(jsonPath("$.games[0].notes").value("Another great game"));
    }

    @Test
    @DisplayName("Should update game in collection successfully")
    @Transactional
    void shouldUpdateGameInCollectionSuccessfully() throws Exception {
        // Add a game first
        AddGameToCollectionRequest addRequest = new AddGameToCollectionRequest(
            1003, "Original notes", Set.of("Original")
        );

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated());

        // Update the game
        UpdateGameCollectionRequest updateRequest = new UpdateGameCollectionRequest(
            "Updated notes", Set.of("Updated", "Strategy")
        );

        mockMvc.perform(put(COLLECTION_BASE_URL + "/games/1003")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(1003))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    @Test
    @DisplayName("Should delete game from collection successfully")
    @Transactional
    void shouldDeleteGameFromCollectionSuccessfully() throws Exception {
        // Add a game first
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1004, "To be deleted", Set.of()
        );

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Delete the game
        mockMvc.perform(delete(COLLECTION_BASE_URL + "/games/1004")
                .header(USER_ID_HEADER, userId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify collection is empty
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header(USER_ID_HEADER, userId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games").isEmpty());
    }

    @Test
    @DisplayName("Should return bad request when adding duplicate game")
    @Transactional
    void shouldReturnBadRequestWhenAddingDuplicateGame() throws Exception {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1005, "First addition", Set.of()
        );

        // Add game first time
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to add same game again
        AddGameToCollectionRequest duplicateRequest = new AddGameToCollectionRequest(
            1005, "Duplicate attempt", Set.of()
        );

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request when updating non-existent game")
    @Transactional
    void shouldReturnBadRequestWhenUpdatingNonExistentGame() throws Exception {
        UpdateGameCollectionRequest updateRequest = new UpdateGameCollectionRequest(
            "Updated notes", Set.of("Strategy")
        );

        mockMvc.perform(put(COLLECTION_BASE_URL + "/games/9999")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request when deleting non-existent game")
    @Transactional
    void shouldReturnBadRequestWhenDeletingNonExistentGame() throws Exception {
        mockMvc.perform(delete(COLLECTION_BASE_URL + "/games/9999")
                .header(USER_ID_HEADER, userId)
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle games with multiple labels")
    @Transactional
    void shouldHandleGamesWithMultipleLabels() throws Exception {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1006, "Multi-label game", Set.of("Strategy", "Euro", "Economic", "Medium-Weight")
        );

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(1006))
                .andExpect(jsonPath("$.labels").isArray())
                .andExpect(jsonPath("$.labels.length()").value(4));
    }

    @Test
    @DisplayName("Should handle game with no labels")
    @Transactional
    void shouldHandleGameWithNoLabels() throws Exception {
        AddGameToCollectionRequest request = new AddGameToCollectionRequest(
            1007, "No labels game", null
        );

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .header(USER_ID_HEADER, userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(1007))
                .andExpect(jsonPath("$.labels").isArray());
    }

    @Test
    @DisplayName("Should return bad request when missing X-User-ID header")
    @Transactional
    void shouldReturnBadRequestWhenMissingUserIdHeader() throws Exception {
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .with(csrf()))
                .andExpect(status().isBadRequest());

        AddGameToCollectionRequest request = new AddGameToCollectionRequest(1008, "Test", Set.of());

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle concurrent operations on collection")
    @Transactional
    void shouldHandleConcurrentOperationsOnCollection() throws Exception {
        // Add multiple games quickly to simulate concurrent operations
        for (int i = 2001; i <= 2003; i++) {
            AddGameToCollectionRequest request = new AddGameToCollectionRequest(
                i, "Concurrent game " + i, Set.of("Concurrent")
            );

            mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                    .header(USER_ID_HEADER, userId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.gameId").value(i));
        }

        // Verify all games were added
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header(USER_ID_HEADER, userId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(3));
    }
}