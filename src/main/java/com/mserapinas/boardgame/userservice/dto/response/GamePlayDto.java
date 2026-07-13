package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.GamePlay;
import com.mserapinas.boardgame.userservice.model.GamePlayWinner;
import com.mserapinas.boardgame.userservice.model.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GamePlayDto(
    Long id,
    Long loggerId,
    Integer gameId,
    LocalDate playedAt,
    Integer timesPlayed,
    Integer durationMinutes,
    List<PlayerRef> players,
    List<List<PlayerRef>> winners,
    String notes,
    OffsetDateTime createdAt
) {
    public record PlayerRef(Long id, String name) {
        public static PlayerRef from(User user) {
            return new PlayerRef(user.getId(), user.getName());
        }
    }

    public static GamePlayDto from(GamePlay gp) {
        Map<Integer, List<PlayerRef>> byIndex = new LinkedHashMap<>();
        for (GamePlayWinner w : gp.getWinnersOrdered()) {
            byIndex
                .computeIfAbsent(w.getGameIndex(), k -> new ArrayList<>())
                .add(w.getWinner() == null ? null : PlayerRef.from(w.getWinner()));
        }

        List<List<PlayerRef>> winners = new ArrayList<>();
        for (int i = 0; i < gp.getTimesPlayed(); i++) {
            winners.add(byIndex.getOrDefault(i, List.of()));
        }

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
