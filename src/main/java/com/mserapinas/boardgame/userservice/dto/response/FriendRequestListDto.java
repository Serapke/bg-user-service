package com.mserapinas.boardgame.userservice.dto.response;

import java.util.List;

public record FriendRequestListDto(
    List<FriendRequestDto> requests,
    Long totalCount
) {
}