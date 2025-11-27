package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.UserBoardGame;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record GameCollectionItemDto(
    Integer gameId,
    String notes,
    Integer userRating,
    OffsetDateTime modifiedAt,
    Set<LabelDto> labels
) {
    public static GameCollectionItemDto from(UserBoardGame userBoardGame, Integer userRating) {
        Set<LabelDto> labelDtos = userBoardGame.getLabels() != null ?
            userBoardGame.getLabels().stream()
                .map(LabelDto::from)
                .collect(Collectors.toSet()) :
            Set.of();

        return new GameCollectionItemDto(
            userBoardGame.getGameId(),
            userBoardGame.getNotes(),
            userRating,
            userBoardGame.getModifiedAt(),
            labelDtos
        );
    }
}