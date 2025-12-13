package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.Friendship;

import java.util.List;

public record FriendListDto(
    List<FriendDto> friends,
    Long totalCount
) {
    public static FriendListDto from(List<Friendship> friendships) {
        List<FriendDto> friends = friendships.stream()
            .map(FriendDto::from)
            .toList();
        return new FriendListDto(friends, (long) friends.size());
    }
}