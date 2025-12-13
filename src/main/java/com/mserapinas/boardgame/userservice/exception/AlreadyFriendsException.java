package com.mserapinas.boardgame.userservice.exception;

public class AlreadyFriendsException extends RuntimeException {
    private final Long userId;
    private final Long friendId;

    public AlreadyFriendsException(Long userId, Long friendId) {
        super("These users are already friends");
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
