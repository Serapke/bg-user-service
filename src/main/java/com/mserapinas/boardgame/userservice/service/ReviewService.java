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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewDto createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (reviewRepository.existsByUserIdAndGameId(userId, request.gameId())) {
            throw new ReviewAlreadyExistsException(userId, request.gameId());
        }

        Review review = new Review(
            user,
            request.gameId(),
            request.rating(),
            request.reviewText()
        );

        Review savedReview = reviewRepository.save(review);

        // Fetch with user to populate the relationship
        Review reviewWithUser = reviewRepository.findByIdWithUser(savedReview.getId())
            .orElseThrow(() -> new ReviewNotFoundException(savedReview.getId()));

        return ReviewDto.from(reviewWithUser);
    }

    public ReviewDto getReviewById(Long reviewId) {
        Review review = reviewRepository.findByIdWithUser(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        return ReviewDto.from(review);
    }

    public List<ReviewDto> getReviewsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        return reviewRepository.findByUserIdWithUser(userId)
            .stream()
            .map(ReviewDto::from)
            .toList();
    }

    public ReviewListDto getReviewsByGame(Integer gameId) {
        List<Review> reviewEntities = reviewRepository.findByGameIdWithUser(gameId);

        List<ReviewDto> reviews = reviewEntities.stream()
            .map(ReviewDto::from)
            .toList();

        Long totalCount = (long) reviewEntities.size();
        Double averageRating = reviewEntities.stream()
            .mapToInt(Review::getRating)
            .average()
            .stream()
            .boxed()
            .findFirst()
            .orElse(null);

        return new ReviewListDto(reviews, totalCount, averageRating);
    }

    @Transactional
    public ReviewDto updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewAccessException(reviewId, userId);
        }

        review.setRating(request.rating());
        review.setReviewText(request.reviewText());

        Review savedReview = reviewRepository.save(review);

        // Fetch with user to populate the relationship
        Review reviewWithUser = reviewRepository.findByIdWithUser(savedReview.getId())
            .orElseThrow(() -> new ReviewNotFoundException(savedReview.getId()));

        return ReviewDto.from(reviewWithUser);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewAccessException(reviewId, userId);
        }

        reviewRepository.delete(review);
    }
}