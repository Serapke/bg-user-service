package com.mserapinas.boardgame.userservice.exception;

public class SelfFriendshipException extends RuntimeException {
    public SelfFriendshipException() {
        super("Users cannot send friend requests to themselves");
    }
}