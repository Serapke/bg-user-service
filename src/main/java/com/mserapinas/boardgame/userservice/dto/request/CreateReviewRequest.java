package com.mserapinas.boardgame.userservice.dto.request;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(
    @NotNull(message = "Game ID is required")
    @Positive(message = "Game ID must be positive")
    Integer gameId,

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    Integer rating,

    @Size(max = 2000, message = "Review text cannot exceed 2000 characters")
    String reviewText
) {
}