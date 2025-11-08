package com.mserapinas.boardgame.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mserapinas.boardgame.userservice.dto.request.CreateReviewRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateReviewRequest;
import com.mserapinas.boardgame.userservice.dto.response.ReviewDto;
import com.mserapinas.boardgame.userservice.dto.response.ReviewListDto;
import com.mserapinas.boardgame.userservice.exception.ReviewAlreadyExistsException;
import com.mserapinas.boardgame.userservice.exception.ReviewNotFoundException;
import com.mserapinas.boardgame.userservice.exception.UnauthorizedReviewAccessException;
import com.mserapinas.boardgame.userservice.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("DataFlowIssue")
@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    private static final String BASE_URL = "/api/v1/reviews";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_REVIEW_ID = 1L;
    private static final Integer TEST_GAME_ID = 1001;

    @Test
    @DisplayName("Should create review successfully")
    void shouldCreateReviewSuccessfully() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Amazing game!");
        ReviewDto reviewDto = new ReviewDto(
            TEST_REVIEW_ID,
            TEST_USER_ID,
            "Test User",
            TEST_GAME_ID,
            5,
            "Amazing game!",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );

        when(reviewService.createReview(eq(TEST_USER_ID), any(CreateReviewRequest.class)))
            .thenReturn(reviewDto);

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_REVIEW_ID))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.gameId").value(TEST_GAME_ID))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewText").value("Amazing game!"));

        verify(reviewService).createReview(eq(TEST_USER_ID), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when creating review without game ID")
    void shouldReturnBadRequestWhenCreatingReviewWithoutGameId() throws Exception {
        CreateReviewRequest invalidRequest = new CreateReviewRequest(null, 5, "Review");

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(Long.class), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when creating review without rating")
    void shouldReturnBadRequestWhenCreatingReviewWithoutRating() throws Exception {
        CreateReviewRequest invalidRequest = new CreateReviewRequest(TEST_GAME_ID, null, "Review");

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(Long.class), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when rating is below minimum")
    void shouldReturnBadRequestWhenRatingIsBelowMinimum() throws Exception {
        CreateReviewRequest invalidRequest = new CreateReviewRequest(TEST_GAME_ID, 0, "Review");

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(Long.class), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when rating is above maximum")
    void shouldReturnBadRequestWhenRatingIsAboveMaximum() throws Exception {
        CreateReviewRequest invalidRequest = new CreateReviewRequest(TEST_GAME_ID, 6, "Review");

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(Long.class), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should create review without review text")
    void shouldCreateReviewWithoutReviewText() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 4, null);
        ReviewDto reviewDto = new ReviewDto(
            TEST_REVIEW_ID,
            TEST_USER_ID,
            "Test User",
            TEST_GAME_ID,
            4,
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );

        when(reviewService.createReview(eq(TEST_USER_ID), any(CreateReviewRequest.class)))
            .thenReturn(reviewDto);

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.reviewText").isEmpty());

        verify(reviewService).createReview(eq(TEST_USER_ID), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return conflict when creating duplicate review")
    void shouldReturnConflictWhenCreatingDuplicateReview() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Review");

        when(reviewService.createReview(eq(TEST_USER_ID), any(CreateReviewRequest.class)))
            .thenThrow(new ReviewAlreadyExistsException(TEST_USER_ID, TEST_GAME_ID));

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(reviewService).createReview(eq(TEST_USER_ID), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should get review by ID successfully")
    void shouldGetReviewByIdSuccessfully() throws Exception {
        ReviewDto reviewDto = new ReviewDto(
            TEST_REVIEW_ID,
            TEST_USER_ID,
            "Test User",
            TEST_GAME_ID,
            5,
            "Great game!",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );

        when(reviewService.getReviewById(TEST_REVIEW_ID)).thenReturn(reviewDto);

        mockMvc.perform(get(BASE_URL + "/{reviewId}", TEST_REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(TEST_REVIEW_ID))
                .andExpect(jsonPath("$.userName").value("Test User"));

        verify(reviewService).getReviewById(TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should return not found when getting non-existent review")
    void shouldReturnNotFoundWhenGettingNonExistentReview() throws Exception {
        when(reviewService.getReviewById(TEST_REVIEW_ID))
            .thenThrow(new ReviewNotFoundException(TEST_REVIEW_ID));

        mockMvc.perform(get(BASE_URL + "/{reviewId}", TEST_REVIEW_ID))
                .andExpect(status().isNotFound());

        verify(reviewService).getReviewById(TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should get current user reviews successfully")
    void shouldGetCurrentUserReviewsSuccessfully() throws Exception {
        ReviewDto reviewDto = new ReviewDto(
            TEST_REVIEW_ID,
            TEST_USER_ID,
            "Test User",
            TEST_GAME_ID,
            5,
            "Great!",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );

        when(reviewService.getReviewsByUser(TEST_USER_ID)).thenReturn(List.of(reviewDto));

        mockMvc.perform(get(BASE_URL + "/me")
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(TEST_REVIEW_ID));

        verify(reviewService).getReviewsByUser(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should get reviews for game successfully")
    void shouldGetReviewsForGameSuccessfully() throws Exception {
        ReviewDto reviewDto = new ReviewDto(
            TEST_REVIEW_ID,
            TEST_USER_ID,
            "Test User",
            TEST_GAME_ID,
            5,
            "Great!",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
        ReviewListDto reviewListDto = new ReviewListDto(List.of(reviewDto), 1L, 5.0);

        when(reviewService.getReviewsByGame(TEST_GAME_ID)).thenReturn(reviewListDto);

        mockMvc.perform(get(BASE_URL + "/games/{gameId}", TEST_GAME_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0));

        verify(reviewService).getReviewsByGame(TEST_GAME_ID);
    }

    @Test
    @DisplayName("Should update review successfully")
    void shouldUpdateReviewSuccessfully() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated review");
        ReviewDto reviewDto = new ReviewDto(
            TEST_REVIEW_ID,
            TEST_USER_ID,
            "Test User",
            TEST_GAME_ID,
            4,
            "Updated review",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );

        when(reviewService.updateReview(eq(TEST_USER_ID), eq(TEST_REVIEW_ID), any(UpdateReviewRequest.class)))
            .thenReturn(reviewDto);

        mockMvc.perform(put(BASE_URL + "/{reviewId}", TEST_REVIEW_ID)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.reviewText").value("Updated review"));

        verify(reviewService).updateReview(eq(TEST_USER_ID), eq(TEST_REVIEW_ID), any(UpdateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return forbidden when updating another user's review")
    void shouldReturnForbiddenWhenUpdatingAnotherUsersReview() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated");

        when(reviewService.updateReview(eq(TEST_USER_ID), eq(TEST_REVIEW_ID), any(UpdateReviewRequest.class)))
            .thenThrow(new UnauthorizedReviewAccessException(TEST_REVIEW_ID, TEST_USER_ID));

        mockMvc.perform(put(BASE_URL + "/{reviewId}", TEST_REVIEW_ID)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(reviewService).updateReview(eq(TEST_USER_ID), eq(TEST_REVIEW_ID), any(UpdateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return not found when updating non-existent review")
    void shouldReturnNotFoundWhenUpdatingNonExistentReview() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated");

        when(reviewService.updateReview(eq(TEST_USER_ID), eq(TEST_REVIEW_ID), any(UpdateReviewRequest.class)))
            .thenThrow(new ReviewNotFoundException(TEST_REVIEW_ID));

        mockMvc.perform(put(BASE_URL + "/{reviewId}", TEST_REVIEW_ID)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(reviewService).updateReview(eq(TEST_USER_ID), eq(TEST_REVIEW_ID), any(UpdateReviewRequest.class));
    }

    @Test
    @DisplayName("Should delete review successfully")
    void shouldDeleteReviewSuccessfully() throws Exception {
        doNothing().when(reviewService).deleteReview(TEST_USER_ID, TEST_REVIEW_ID);

        mockMvc.perform(delete(BASE_URL + "/{reviewId}", TEST_REVIEW_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(TEST_USER_ID, TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should return forbidden when deleting another user's review")
    void shouldReturnForbiddenWhenDeletingAnotherUsersReview() throws Exception {
        doThrow(new UnauthorizedReviewAccessException(TEST_REVIEW_ID, TEST_USER_ID))
            .when(reviewService).deleteReview(TEST_USER_ID, TEST_REVIEW_ID);

        mockMvc.perform(delete(BASE_URL + "/{reviewId}", TEST_REVIEW_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isForbidden());

        verify(reviewService).deleteReview(TEST_USER_ID, TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should return not found when deleting non-existent review")
    void shouldReturnNotFoundWhenDeletingNonExistentReview() throws Exception {
        doThrow(new ReviewNotFoundException(TEST_REVIEW_ID))
            .when(reviewService).deleteReview(TEST_USER_ID, TEST_REVIEW_ID);

        mockMvc.perform(delete(BASE_URL + "/{reviewId}", TEST_REVIEW_ID)
                .header(USER_ID_HEADER, TEST_USER_ID))
                .andExpect(status().isNotFound());

        verify(reviewService).deleteReview(TEST_USER_ID, TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should return bad request when creating review without X-User-ID header")
    void shouldReturnBadRequestWhenCreatingReviewWithoutHeader() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Review");

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(Long.class), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should handle malformed JSON request")
    void shouldHandleMalformedJsonRequest() throws Exception {
        String malformedJson = "{\"rating\":}";

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(Long.class), any(CreateReviewRequest.class));
    }

    @Test
    @DisplayName("Should return bad request when review text exceeds max length")
    void shouldReturnBadRequestWhenReviewTextExceedsMaxLength() throws Exception {
        String longReviewText = "a".repeat(2001); // Exceeds 2000 character limit
        CreateReviewRequest invalidRequest = new CreateReviewRequest(TEST_GAME_ID, 5, longReviewText);

        mockMvc.perform(post(BASE_URL)
                .header(USER_ID_HEADER, TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(Long.class), any(CreateReviewRequest.class));
    }
}