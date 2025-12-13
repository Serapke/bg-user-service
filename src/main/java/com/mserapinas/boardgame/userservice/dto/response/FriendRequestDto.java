package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.Friendship;

import java.time.OffsetDateTime;

public record FriendRequestDto(
    Long requestId,
    Long userId,
    String userName,
    String email,
    OffsetDateTime requestedAt,
    FriendRequestDirection direction
) {
    public static FriendRequestDto fromIncoming(Friendship friendship) {
        return new FriendRequestDto(
            friendship.getId(),
            friendship.getUserId(),
            friendship.getUser() != null ? friendship.getUser().getName() : null,
            friendship.getUser() != null ? friendship.getUser().getEmail() : null,
            friendship.getCreatedAt(),
            FriendRequestDirection.INCOMING
        );
    }

    public static FriendRequestDto fromOutgoing(Friendship friendship) {
        return new FriendRequestDto(
            friendship.getId(),
            friendship.getFriendId(),
            friendship.getFriend() != null ? friendship.getFriend().getName() : null,
            friendship.getFriend() != null ? friendship.getFriend().getEmail() : null,
            friendship.getCreatedAt(),
            FriendRequestDirection.OUTGOING
        );
    }
}