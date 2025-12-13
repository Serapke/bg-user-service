package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.Friendship;

import java.time.OffsetDateTime;

public record FriendDto(
    Long userId,
    String userName,
    String email,
    OffsetDateTime friendsSince
) {
    public static FriendDto from(Friendship friendship) {
        return new FriendDto(
            friendship.getFriendId(),
            friendship.getFriend() != null ? friendship.getFriend().getName() : null,
            friendship.getFriend() != null ? friendship.getFriend().getEmail() : null,
            friendship.getCreatedAt()
        );
    }
}