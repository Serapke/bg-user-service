package com.mserapinas.boardgame.userservice.exception;

public class PlayerGroupAccessForbiddenException extends RuntimeException {
    public PlayerGroupAccessForbiddenException(Long groupId, Long userId) {
        super("User ID '" + userId + "' is not authorized to access player group ID '" + groupId + "'");
    }
}
