package com.mserapinas.boardgame.userservice.exception;

public class UnauthorizedReviewAccessException extends RuntimeException {
    public UnauthorizedReviewAccessException(Long reviewId, Long userId) {
        super("User ID '" + userId + "' is not authorized to access review ID '" + reviewId + "'");
    }
}