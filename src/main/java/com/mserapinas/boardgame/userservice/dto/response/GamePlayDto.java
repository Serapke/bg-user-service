package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.GamePlay;
import com.mserapinas.boardgame.userservice.model.GamePlayWinner;
import com.mserapinas.boardgame.userservice.model.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record GamePlayDto(
    Long id,
    Long loggerId,
    Integer gameId,
    LocalDate playedAt,
    Integer timesPlayed,
    Integer durationMinutes,
    List<PlayerRef> players,
    List<PlayerRef> winners,
    String notes,
    OffsetDateTime createdAt
) {
    public record PlayerRef(Long id, String name) {
        public static PlayerRef from(User user) {
            return new PlayerRef(user.getId(), user.getName());
        }
    }

    public static GamePlayDto from(GamePlay gp) {
        List<PlayerRef> winners = gp.getWinnersOrdered().stream()
            .map(GamePlayWinner::getWinner)
            .map(u -> u == null ? null : PlayerRef.from(u))
            .toList();

        return new GamePlayDto(
            gp.getId(),
            gp.getLoggerId(),
            gp.getGameId(),
            gp.getPlayedAt(),
            gp.getTimesPlayed(),
            gp.getDurationMinutes(),
            gp.getPlayers().stream().map(PlayerRef::from).toList(),
            winners,
            gp.getNotes(),
            gp.getCreatedAt()
        );
    }
}
