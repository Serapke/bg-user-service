package com.mserapinas.boardgame.userservice.integration;

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

    private static final String COLLECTION_BASE_URL = "/api/v1/collection";
    private static final String AUTH_BASE_URL = "/api/v1/auth";
    
    private String accessToken;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        // Register a user and get access token
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "Test User", "Password123!");
        
        MvcResult result = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(response);
        accessToken = jsonNode.get("token").asText();
    }

    @Test
    @DisplayName("Should manage complete game collection lifecycle")
    @Transactional
    void shouldManageCompleteGameCollectionLifecycle() throws Exception {
        // Step 1: Get empty collection initially
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games").isEmpty());

        // Step 2: Add first game with labels
        AddGameToCollectionRequest addRequest1 = new AddGameToCollectionRequest(
            1001, "Great strategy game", Set.of("Strategy", "Euro")
        );
        
        MvcResult addResult1 = mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(1001))
                .andExpect(jsonPath("$.notes").value("Great strategy game"))
                .andExpect(jsonPath("$.labels").isArray())
                .andReturn();

        // Step 3: Add second game without labels
        AddGameToCollectionRequest addRequest2 = new AddGameToCollectionRequest(
            1002, "Fun party game", null
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(1002))
                .andExpect(jsonPath("$.notes").value("Fun party game"));

        // Step 4: Get collection with both games
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games.length()").value(2));

        // Step 5: Update first game
        UpdateGameCollectionRequest updateRequest = new UpdateGameCollectionRequest(
            "Updated notes for this amazing game", Set.of("Strategy", "Updated")
        );
        
        mockMvc.perform(put(COLLECTION_BASE_URL + "/games/1001")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(1001))
                .andExpect(jsonPath("$.notes").value("Updated notes for this amazing game"));

        // Step 6: Try to add duplicate game (should fail)
        AddGameToCollectionRequest duplicateRequest = new AddGameToCollectionRequest(
            1001, "Duplicate attempt", Set.of()
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());

        // Step 7: Delete a game
        mockMvc.perform(delete(COLLECTION_BASE_URL + "/games/1002")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Step 8: Verify collection has only one game left
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games.length()").value(1))
                .andExpect(jsonPath("$.games[0].gameId").value(1001));

        // Step 9: Try to delete non-existent game (should fail)
        mockMvc.perform(delete(COLLECTION_BASE_URL + "/games/9999")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest());

        // Step 10: Try to update non-existent game (should fail)
        UpdateGameCollectionRequest nonExistentUpdateRequest = new UpdateGameCollectionRequest(
            "This should fail", Set.of()
        );
        
        mockMvc.perform(put(COLLECTION_BASE_URL + "/games/9999")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUpdateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle label management correctly")
    @Transactional
    void shouldHandleLabelManagementCorrectly() throws Exception {
        // Add game with initial labels
        AddGameToCollectionRequest addRequest = new AddGameToCollectionRequest(
            1001, "Game with labels", Set.of("Strategy", "Heavy", "Euro")
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.labels.length()").value(3));

        // Update with some overlapping labels (should reuse existing ones)
        UpdateGameCollectionRequest updateRequest = new UpdateGameCollectionRequest(
            "Updated game", Set.of("Strategy", "Updated", "New")
        );
        
        mockMvc.perform(put(COLLECTION_BASE_URL + "/games/1001")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels.length()").value(3));

        // Add another game with some of the same labels
        AddGameToCollectionRequest addRequest2 = new AddGameToCollectionRequest(
            1002, "Another game", Set.of("Strategy", "Light")
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.labels.length()").value(2));
    }

    @Test
    @DisplayName("Should enforce validation rules on collection operations")
    @Transactional
    void shouldEnforceValidationRulesOnCollectionOperations() throws Exception {
        // Test adding game with invalid gameId (negative)
        AddGameToCollectionRequest invalidGameIdRequest = new AddGameToCollectionRequest(
            -1, "Invalid game", Set.of()
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidGameIdRequest)))
                .andExpect(status().isBadRequest());

        // Test adding game with null gameId
        String requestWithNullGameId = "{\"gameId\": null, \"notes\": \"Test\", \"labelNames\": []}";
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithNullGameId))
                .andExpect(status().isBadRequest());

        // Test adding game with too long notes
        String longNotes = "a".repeat(1001);
        AddGameToCollectionRequest longNotesRequest = new AddGameToCollectionRequest(
            1001, longNotes, Set.of()
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longNotesRequest)))
                .andExpect(status().isBadRequest());

        // Test adding game with too many labels
        Set<String> tooManyLabels = Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        AddGameToCollectionRequest tooManyLabelsRequest = new AddGameToCollectionRequest(
            1001, "Test", tooManyLabels
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tooManyLabelsRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should require authentication for all collection endpoints")
    @Transactional
    void shouldRequireAuthenticationForAllCollectionEndpoints() throws Exception {
        AddGameToCollectionRequest addRequest = new AddGameToCollectionRequest(1001, "Test", Set.of());
        UpdateGameCollectionRequest updateRequest = new UpdateGameCollectionRequest("Update", Set.of());

        // Test all endpoints without authentication
        mockMvc.perform(get(COLLECTION_BASE_URL))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(COLLECTION_BASE_URL + "/games/1001")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(COLLECTION_BASE_URL + "/games/1001")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should isolate collections between different users")
    @Transactional
    void shouldIsolateCollectionsBetweenDifferentUsers() throws Exception {
        // User 1 adds a game
        AddGameToCollectionRequest user1GameRequest = new AddGameToCollectionRequest(
            1001, "User 1 game", Set.of("User1")
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1GameRequest)))
                .andExpect(status().isCreated());

        // Create a second user
        RegisterRequest user2RegisterRequest = new RegisterRequest("user2@example.com", "User Two", "Password123!");
        
        MvcResult user2Result = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2RegisterRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String user2Response = user2Result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode user2JsonNode = objectMapper.readTree(user2Response);
        String user2AccessToken = user2JsonNode.get("token").asText();

        // User 2 should see empty collection
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header("Authorization", "Bearer " + user2AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games").isEmpty());

        // User 2 can add the same gameId (different user collections)
        AddGameToCollectionRequest user2GameRequest = new AddGameToCollectionRequest(
            1001, "User 2 version of game", Set.of("User2")
        );
        
        mockMvc.perform(post(COLLECTION_BASE_URL + "/games")
                .with(csrf())
                .header("Authorization", "Bearer " + user2AccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2GameRequest)))
                .andExpect(status().isCreated());

        // Verify both users have their own version
        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].notes").value("User 1 game"));

        mockMvc.perform(get(COLLECTION_BASE_URL)
                .header("Authorization", "Bearer " + user2AccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].notes").value("User 2 version of game"));
    }
}