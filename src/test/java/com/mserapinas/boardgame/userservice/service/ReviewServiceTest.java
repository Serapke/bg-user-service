package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.CreateReviewRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateReviewRequest;
import com.mserapinas.boardgame.userservice.dto.response.ReviewDto;
import com.mserapinas.boardgame.userservice.dto.response.ReviewListDto;
import com.mserapinas.boardgame.userservice.exception.ReviewAlreadyExistsException;
import com.mserapinas.boardgame.userservice.exception.ReviewNotFoundException;
import com.mserapinas.boardgame.userservice.exception.UnauthorizedReviewAccessException;
import com.mserapinas.boardgame.userservice.model.Review;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.ReviewRepository;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    private ReviewService reviewService;

    private User testUser;
    private Review testReview;
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_REVIEW_ID = 1L;
    private static final Integer TEST_GAME_ID = 1001;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, userRepository);

        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setCreatedAt(OffsetDateTime.now());

        testReview = new Review(testUser, TEST_GAME_ID, 5, "Great game!");
        testReview.setId(TEST_REVIEW_ID);
        testReview.setCreatedAt(OffsetDateTime.now());
        testReview.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should create review successfully")
    void shouldCreateReviewSuccessfully() {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Great game!");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(reviewRepository.existsByUserIdAndGameId(TEST_USER_ID, TEST_GAME_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewRepository.findByIdWithUser(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

        ReviewDto result = reviewService.createReview(TEST_USER_ID, request);

        assertNotNull(result);
        assertEquals(TEST_REVIEW_ID, result.id());
        assertEquals(TEST_USER_ID, result.userId());
        assertEquals(TEST_GAME_ID, result.gameId());
        assertEquals(5, result.rating());
        assertEquals("Great game!", result.reviewText());

        verify(userRepository).findById(TEST_USER_ID);
        verify(reviewRepository).existsByUserIdAndGameId(TEST_USER_ID, TEST_GAME_ID);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).findByIdWithUser(TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should throw exception when creating review for non-existent user")
    void shouldThrowExceptionWhenCreatingReviewForNonExistentUser() {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Great game!");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> reviewService.createReview(TEST_USER_ID, request));

        verify(userRepository).findById(TEST_USER_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate review")
    void shouldThrowExceptionWhenCreatingDuplicateReview() {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 5, "Great game!");

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(reviewRepository.existsByUserIdAndGameId(TEST_USER_ID, TEST_GAME_ID)).thenReturn(true);

        assertThrows(ReviewAlreadyExistsException.class,
            () -> reviewService.createReview(TEST_USER_ID, request));

        verify(userRepository).findById(TEST_USER_ID);
        verify(reviewRepository).existsByUserIdAndGameId(TEST_USER_ID, TEST_GAME_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Should get review by ID successfully")
    void shouldGetReviewByIdSuccessfully() {
        when(reviewRepository.findByIdWithUser(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

        ReviewDto result = reviewService.getReviewById(TEST_REVIEW_ID);

        assertNotNull(result);
        assertEquals(TEST_REVIEW_ID, result.id());
        assertEquals(TEST_USER_ID, result.userId());
        assertEquals("Test User", result.userName());

        verify(reviewRepository).findByIdWithUser(TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent review")
    void shouldThrowExceptionWhenGettingNonExistentReview() {
        when(reviewRepository.findByIdWithUser(TEST_REVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
            () -> reviewService.getReviewById(TEST_REVIEW_ID));

        verify(reviewRepository).findByIdWithUser(TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should get reviews by user successfully")
    void shouldGetReviewsByUserSuccessfully() {
        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        when(reviewRepository.findByUserIdWithUser(TEST_USER_ID)).thenReturn(List.of(testReview));

        List<ReviewDto> result = reviewService.getReviewsByUser(TEST_USER_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_REVIEW_ID, result.getFirst().id());

        verify(userRepository).existsById(TEST_USER_ID);
        verify(reviewRepository).findByUserIdWithUser(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should throw exception when getting reviews for non-existent user")
    void shouldThrowExceptionWhenGettingReviewsForNonExistentUser() {
        when(userRepository.existsById(TEST_USER_ID)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> reviewService.getReviewsByUser(TEST_USER_ID));

        verify(userRepository).existsById(TEST_USER_ID);
        verify(reviewRepository, never()).findByUserIdWithUser(any());
    }

    @Test
    @DisplayName("Should get reviews by game with statistics")
    void shouldGetReviewsByGameWithStatistics() {
        when(reviewRepository.findByGameIdWithUser(TEST_GAME_ID)).thenReturn(List.of(testReview));

        ReviewListDto result = reviewService.getReviewsByGame(TEST_GAME_ID);

        assertNotNull(result);
        assertEquals(1, result.reviews().size());
        assertEquals(1L, result.totalCount());
        assertEquals(5.0, result.averageRating());

        verify(reviewRepository).findByGameIdWithUser(TEST_GAME_ID);
    }

    @Test
    @DisplayName("Should handle empty reviews for game")
    void shouldHandleEmptyReviewsForGame() {
        when(reviewRepository.findByGameIdWithUser(TEST_GAME_ID)).thenReturn(List.of());

        ReviewListDto result = reviewService.getReviewsByGame(TEST_GAME_ID);

        assertNotNull(result);
        assertTrue(result.reviews().isEmpty());
        assertEquals(0L, result.totalCount());
        assertNull(result.averageRating());

        verify(reviewRepository).findByGameIdWithUser(TEST_GAME_ID);
    }

    @Test
    @DisplayName("Should update review successfully")
    void shouldUpdateReviewSuccessfully() {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated review");

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewRepository.findByIdWithUser(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

        ReviewDto result = reviewService.updateReview(TEST_USER_ID, TEST_REVIEW_ID, request);

        assertNotNull(result);
        assertEquals(4, testReview.getRating());
        assertEquals("Updated review", testReview.getReviewText());

        verify(reviewRepository).findById(TEST_REVIEW_ID);
        verify(reviewRepository).save(testReview);
        verify(reviewRepository).findByIdWithUser(TEST_REVIEW_ID);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent review")
    void shouldThrowExceptionWhenUpdatingNonExistentReview() {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated");

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
            () -> reviewService.updateReview(TEST_USER_ID, TEST_REVIEW_ID, request));

        verify(reviewRepository).findById(TEST_REVIEW_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Should throw exception when updating review by different user")
    void shouldThrowExceptionWhenUpdatingReviewByDifferentUser() {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Updated");
        Long differentUserId = 999L;

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

        assertThrows(UnauthorizedReviewAccessException.class,
            () -> reviewService.updateReview(differentUserId, TEST_REVIEW_ID, request));

        verify(reviewRepository).findById(TEST_REVIEW_ID);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Should delete review successfully")
    void shouldDeleteReviewSuccessfully() {
        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

        assertDoesNotThrow(() -> reviewService.deleteReview(TEST_USER_ID, TEST_REVIEW_ID));

        verify(reviewRepository).findById(TEST_REVIEW_ID);
        verify(reviewRepository).delete(testReview);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent review")
    void shouldThrowExceptionWhenDeletingNonExistentReview() {
        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
            () -> reviewService.deleteReview(TEST_USER_ID, TEST_REVIEW_ID));

        verify(reviewRepository).findById(TEST_REVIEW_ID);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting review by different user")
    void shouldThrowExceptionWhenDeletingReviewByDifferentUser() {
        Long differentUserId = 999L;

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

        assertThrows(UnauthorizedReviewAccessException.class,
            () -> reviewService.deleteReview(differentUserId, TEST_REVIEW_ID));

        verify(reviewRepository).findById(TEST_REVIEW_ID);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("Should create review without review text")
    void shouldCreateReviewWithoutReviewText() {
        CreateReviewRequest request = new CreateReviewRequest(TEST_GAME_ID, 4, null);
        Review reviewWithoutText = new Review(testUser, TEST_GAME_ID, 4, null);
        reviewWithoutText.setId(TEST_REVIEW_ID);
        reviewWithoutText.setCreatedAt(OffsetDateTime.now());
        reviewWithoutText.setUpdatedAt(OffsetDateTime.now());

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(reviewRepository.existsByUserIdAndGameId(TEST_USER_ID, TEST_GAME_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(reviewWithoutText);
        when(reviewRepository.findByIdWithUser(TEST_REVIEW_ID)).thenReturn(Optional.of(reviewWithoutText));

        ReviewDto result = reviewService.createReview(TEST_USER_ID, request);

        assertNotNull(result);
        assertEquals(4, result.rating());
        assertNull(result.reviewText());

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Should update review with null text")
    void shouldUpdateReviewWithNullText() {
        UpdateReviewRequest request = new UpdateReviewRequest(3, null);

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewRepository.findByIdWithUser(TEST_REVIEW_ID)).thenReturn(Optional.of(testReview));

        ReviewDto result = reviewService.updateReview(TEST_USER_ID, TEST_REVIEW_ID, request);

        assertNotNull(result);
        assertEquals(3, testReview.getRating());
        assertNull(testReview.getReviewText());

        verify(reviewRepository).save(testReview);
    }
}