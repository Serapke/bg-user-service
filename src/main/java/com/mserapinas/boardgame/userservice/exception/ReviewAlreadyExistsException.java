package com.mserapinas.boardgame.userservice.exception;

public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(Long userId, Integer gameId) {
        super("Review already exists for user ID '" + userId + "' and game ID '" + gameId + "'");
    }
}