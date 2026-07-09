package com.mserapinas.boardgame.userservice.exception;

public class UnauthorizedGamePlayAccessException extends RuntimeException {
    public UnauthorizedGamePlayAccessException(Long playId, Long userId) {
        super("User ID '" + userId + "' is not authorized to access play ID '" + playId + "'");
    }
}
