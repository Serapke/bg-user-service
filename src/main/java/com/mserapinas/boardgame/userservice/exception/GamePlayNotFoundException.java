package com.mserapinas.boardgame.userservice.exception;

public class GamePlayNotFoundException extends RuntimeException {
    public GamePlayNotFoundException(Long id) {
        super("Game play with ID '" + id + "' not found");
    }
}
