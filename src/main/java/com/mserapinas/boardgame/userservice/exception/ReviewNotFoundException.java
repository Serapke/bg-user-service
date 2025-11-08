package com.mserapinas.boardgame.userservice.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(Long reviewId) {
        super("Review with ID '" + reviewId + "' not found");
    }

}