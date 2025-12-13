package com.mserapinas.boardgame.userservice.exception;

public class FriendRequestAlreadySentException extends RuntimeException {
    private final Long userId;
    private final Long friendId;

    public FriendRequestAlreadySentException(Long userId, Long friendId) {
        super("Friend request has already been sent");
        this.userId = userId;
        this.friendId = friendId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getFriendId() {
        return friendId;
    }
}