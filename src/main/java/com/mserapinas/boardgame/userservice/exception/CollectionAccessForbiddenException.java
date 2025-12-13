package com.mserapinas.boardgame.userservice.exception;

public class CollectionAccessForbiddenException extends RuntimeException {
    public CollectionAccessForbiddenException() {
        super("You do not have permission to view this collection");
    }
}
