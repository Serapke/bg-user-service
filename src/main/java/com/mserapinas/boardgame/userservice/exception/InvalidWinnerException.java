package com.mserapinas.boardgame.userservice.exception;

public class InvalidWinnerException extends RuntimeException {
    public InvalidWinnerException(String message) {
        super(message);
    }
}
