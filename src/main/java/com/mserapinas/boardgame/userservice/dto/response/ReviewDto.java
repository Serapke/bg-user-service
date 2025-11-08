package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.Review;

import java.time.OffsetDateTime;

public record ReviewDto(
    Long id,
    Long userId,
    String userName,
    Integer gameId,
    Integer rating,
    String reviewText,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static ReviewDto from(Review review) {
        return new ReviewDto(
            review.getId(),
            review.getUserId(),
            review.getUser() != null ? review.getUser().getName() : null,
            review.getGameId(),
            review.getRating(),
            review.getReviewText(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}