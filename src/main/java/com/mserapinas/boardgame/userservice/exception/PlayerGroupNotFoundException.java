package com.mserapinas.boardgame.userservice.exception;

public class PlayerGroupNotFoundException extends RuntimeException {
    public PlayerGroupNotFoundException(Long id) {
        super("Player group with ID '" + id + "' not found");
    }
}
