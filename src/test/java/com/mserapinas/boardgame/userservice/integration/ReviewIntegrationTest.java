package com.mserapinas.boardgame.userservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.CreateReviewRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("DataFlowIssue")
@AutoConfigureMockMvc
class ReviewIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REVIEW_BASE_URL = "/api/v1/reviews";
    private static final String AUTH_BASE_URL = "/api/v1/auth";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final Integer TEST_GAME_ID = 1001;

    private Long userId1;
    private Long userId2;

    @BeforeEach
    @Transactional
    void setUp() throws Exception {
        // Register first user
        RegisterRequest registerRequest1 = new RegisterRequest("user1@example.com", "User One", "Password123!");
        MvcResult result1 = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest1)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        userId1 = response1.get("id").asLong();

        // Register second user
        RegisterRequest registerRequest2 = new RegisterRequest("user2@example.com", "User Two", "Password123!");
        MvcResult result2 = mockMvc.perform(post(AUTH_BASE_URL + "/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response2 = objectMapper.readTree(result2.getResponse().getContentAsString());
        userId2 = response2.get("id").asLong();
    }

    @Test
    @DisplayName("Should create review successfully")
    @Transactional
    void shouldCreateReviewSuccessfully() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Amazing game!");

        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(userId1))
                .andExpect(jsonPath("$.userName").value("User One"))
                .andExpect(jsonPath("$.gameId").value(TEST_GAME_ID))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewText").value("Amazing game!"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Should create review without review text")
    @Transactional
    void shouldCreateReviewWithoutReviewText() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 4, null);

        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.reviewText").isEmpty());
    }

    @Test
    @DisplayName("Should prevent duplicate review for same user and game")
    @Transactional
    void shouldPreventDuplicateReviewForSameUserAndGame() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "First review");

        // Create first review
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to create duplicate review
        CreateReviewRequest duplicateRequest = new CreateReviewRequest(TEST_GAME_ID, 4, "Second review");

        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Should allow different users to review same game")
    @Transactional
    void shouldAllowDifferentUsersToReviewSameGame() throws Exception {
        // User 1 creates review
        CreateReviewRequest request1 = new CreateReviewRequest(TEST_GAME_ID, 5, "User 1 review");
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // User 2 creates review for same game
        CreateReviewRequest request2 = new CreateReviewRequest(TEST_GAME_ID, 3, "User 2 review");
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId2)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(3));
    }

    @Test
    @DisplayName("Should get review by ID")
    @Transactional
    void shouldGetReviewById() throws Exception {
        // Create review
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Great!");
        MvcResult createResult = mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long reviewId = createResponse.get("id").asLong();

        // Get review by ID
        mockMvc.perform(get(REVIEW_BASE_URL + "/{reviewId}", reviewId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.userName").value("User One"));
    }

    @Test
    @DisplayName("Should get current user reviews")
    @Transactional
    void shouldGetCurrentUserReviews() throws Exception {
        // Create multiple reviews for user 1
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(1001, 5, "Review 1"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(1002, 4, "Review 2"))))
                .andExpect(status().isCreated());

        // Get user's reviews
        mockMvc.perform(get(REVIEW_BASE_URL + "/me")
                .header(USER_ID_HEADER, userId1)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should get reviews for game with statistics")
    @Transactional
    void shouldGetReviewsForGameWithStatistics() throws Exception {
        // User 1 reviews with rating 5
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(TEST_GAME_ID, 5, "Love it!"))))
                .andExpect(status().isCreated());

        // User 2 reviews with rating 3
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId2)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(TEST_GAME_ID, 3, "It's okay"))))
                .andExpect(status().isCreated());

        // Get reviews for game
        mockMvc.perform(get(REVIEW_BASE_URL + "/games/{gameId}", TEST_GAME_ID)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews.length()").value(2))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.averageRating").value(4.0)); // Average of 5 and 3
    }

    @Test
    @DisplayName("Should update review successfully")
    @Transactional
    void shouldUpdateReviewSuccessfully() throws Exception {
        // Create review
        CreateReviewRequest createRequest = new CreateReviewRequest(TEST_GAME_ID, 5, "Original review");
        MvcResult createResult = mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long reviewId = createResponse.get("id").asLong();

        // Update review
        UpdateReviewRequest updateRequest = new UpdateReviewRequest(4, "Updated review");
        mockMvc.perform(put(REVIEW_BASE_URL + "/{reviewId}", reviewId)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.reviewText").value("Updated review"));
    }

    @Test
    @DisplayName("Should prevent updating another user's review")
    @Transactional
    void shouldPreventUpdatingAnotherUsersReview() throws Exception {
        // User 1 creates review
        CreateReviewRequest createRequest = new CreateReviewRequest(TEST_GAME_ID, 5, "User 1 review");
        MvcResult createResult = mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long reviewId = createResponse.get("id").asLong();

        // User 2 tries to update User 1's review
        UpdateReviewRequest updateRequest = new UpdateReviewRequest(1, "Hacked!");
        mockMvc.perform(put(REVIEW_BASE_URL + "/{reviewId}", reviewId)
                .header(USER_ID_HEADER, userId2)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should delete review successfully")
    @Transactional
    void shouldDeleteReviewSuccessfully() throws Exception {
        // Create review
        CreateReviewRequest createRequest = new CreateReviewRequest(TEST_GAME_ID, 5, "To be deleted");
        MvcResult createResult = mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long reviewId = createResponse.get("id").asLong();

        // Delete review
        mockMvc.perform(delete(REVIEW_BASE_URL + "/{reviewId}", reviewId)
                .header(USER_ID_HEADER, userId1)
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify review is deleted
        mockMvc.perform(get(REVIEW_BASE_URL + "/{reviewId}", reviewId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prevent deleting another user's review")
    @Transactional
    void shouldPreventDeletingAnotherUsersReview() throws Exception {
        // User 1 creates review
        CreateReviewRequest createRequest = new CreateReviewRequest(TEST_GAME_ID, 5, "User 1 review");
        MvcResult createResult = mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long reviewId = createResponse.get("id").asLong();

        // User 2 tries to delete User 1's review
        mockMvc.perform(delete(REVIEW_BASE_URL + "/{reviewId}", reviewId)
                .header(USER_ID_HEADER, userId2)
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should validate rating constraints")
    @Transactional
    void shouldValidateRatingConstraints() throws Exception {
        // Rating below minimum
        CreateReviewRequest request1 = new CreateReviewRequest(TEST_GAME_ID, 0, "Invalid");
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isBadRequest());

        // Rating above maximum
        CreateReviewRequest request2 = new CreateReviewRequest(TEST_GAME_ID, 6, "Invalid");
        mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty game reviews")
    @Transactional
    void shouldHandleEmptyGameReviews() throws Exception {
        Integer gameWithNoReviews = 9999;

        mockMvc.perform(get(REVIEW_BASE_URL + "/games/{gameId}", gameWithNoReviews)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews").isEmpty())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.averageRating").isEmpty());
    }

    @Test
    @DisplayName("Should cascade delete reviews when user is deleted")
    @Transactional
    void shouldCascadeDeleteReviewsWhenUserIsDeleted() throws Exception {
        // User 1 creates review
        CreateReviewRequest createRequest = new CreateReviewRequest(TEST_GAME_ID, 5, "Great game!");
        MvcResult createResult = mockMvc.perform(post(REVIEW_BASE_URL)
                .header(USER_ID_HEADER, userId1)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long reviewId = createResponse.get("id").asLong();

        // Delete user account
        mockMvc.perform(delete("/api/v1/users/me")
                .header(USER_ID_HEADER, userId1)
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify review is also deleted
        mockMvc.perform(get(REVIEW_BASE_URL + "/{reviewId}", reviewId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}