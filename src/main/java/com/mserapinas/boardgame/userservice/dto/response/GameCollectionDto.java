package com.mserapinas.boardgame.userservice.dto.response;

import java.util.List;

public record GameCollectionDto(
    List<GameCollectionItemDto> games
) {
    public static GameCollectionDto from(List<GameCollectionItemDto> games) {
        return new GameCollectionDto(games);
    }
}