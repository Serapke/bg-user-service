package com.mserapinas.boardgame.userservice.exception;

public class FriendRequestNotFoundException extends RuntimeException {
    private final Long userId;
    private final Long friendId;

    public FriendRequestNotFoundException(Long userId, Long friendId) {
        super("Friend request not found");
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