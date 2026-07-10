package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.dto.request.CreateGamePlayRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateGamePlayRequest;
import com.mserapinas.boardgame.userservice.dto.response.GamePlayDto;
import com.mserapinas.boardgame.userservice.exception.GamePlayNotFoundException;
import com.mserapinas.boardgame.userservice.exception.InvalidWinnerException;
import com.mserapinas.boardgame.userservice.exception.UnauthorizedGamePlayAccessException;
import com.mserapinas.boardgame.userservice.model.GamePlay;
import com.mserapinas.boardgame.userservice.model.GamePlayWinner;
import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.GamePlayRepository;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GamePlayService {

    private final GamePlayRepository gamePlayRepository;
    private final UserRepository userRepository;

    public GamePlayService(GamePlayRepository gamePlayRepository, UserRepository userRepository) {
        this.gamePlayRepository = gamePlayRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GamePlayDto createGamePlay(Long userId, CreateGamePlayRequest request) {
        User logger = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<User> players = new HashSet<>();
        if (request.playerIds() != null && !request.playerIds().isEmpty()) {
            players.addAll(userRepository.findAllById(request.playerIds()));
            if (players.size() != request.playerIds().size()) {
                throw new IllegalArgumentException("One or more player IDs are invalid");
            }
        }

        List<Long> winnerIds = request.winnerPlayerIds() == null ? List.of() : request.winnerPlayerIds();
        if (!winnerIds.isEmpty() && winnerIds.size() != request.timesPlayed()) {
            throw new InvalidWinnerException(
                "Winner count must match times played");
        }

        Map<Long, User> playersById = players.stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        GamePlay gp = new GamePlay();
        gp.setLogger(logger);
        gp.setGameId(request.gameId());
        gp.setPlayedAt(request.playedAt());
        gp.setTimesPlayed(request.timesPlayed());
        gp.setDurationMinutes(request.durationMinutes());
        gp.setPlayers(players);
        gp.setNotes(request.notes());

        GamePlay saved = gamePlayRepository.save(gp);

        if (!winnerIds.isEmpty()) {
            List<GamePlayWinner> winners = new ArrayList<>();
            for (int i = 0; i < winnerIds.size(); i++) {
                Long winnerId = winnerIds.get(i);
                User winner = null;
                if (winnerId != null) {
                    winner = playersById.get(winnerId);
                    if (winner == null) {
                        throw new InvalidWinnerException(
                            "Winner must be one of the players who played");
                    }
                }
                winners.add(new GamePlayWinner(saved, i, winner));
            }
            saved.setWinners(winners);
            saved = gamePlayRepository.save(saved);
        }

        Long savedId = saved.getId();
        return GamePlayDto.from(
            gamePlayRepository.findByIdWithAssociations(savedId)
                .orElseThrow(() -> new GamePlayNotFoundException(savedId))
        );
    }

    public List<GamePlayDto> getPlaysForGame(Long userId, Integer gameId) {
        return gamePlayRepository.findByLoggerAndGame(userId, gameId).stream()
            .map(GamePlayDto::from)
            .toList();
    }

    public List<GamePlayDto> getRecentGames(Long userId, int limit) {
        return gamePlayRepository.findRecentByUser(userId, PageRequest.of(0, limit * 3))
            .stream()
            .collect(Collectors.toMap(
                GamePlay::getGameId,
                gp -> gp,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ))
            .values()
            .stream()
            .limit(limit)
            .map(GamePlayDto::from)
            .toList();
    }

    public List<GamePlayDto> getPlaysByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return gamePlayRepository.findByIds(ids).stream()
            .map(GamePlayDto::from)
            .toList();
    }

    @Transactional
    public GamePlayDto updateGamePlay(Long userId, Long playId, UpdateGamePlayRequest request) {
        GamePlay gp = gamePlayRepository.findByIdWithAssociations(playId)
            .orElseThrow(() -> new GamePlayNotFoundException(playId));

        if (!gp.getLogger().getId().equals(userId)) {
            throw new UnauthorizedGamePlayAccessException(playId, userId);
        }

        Set<User> players = new HashSet<>();
        if (request.playerIds() != null && !request.playerIds().isEmpty()) {
            players.addAll(userRepository.findAllById(request.playerIds()));
            if (players.size() != request.playerIds().size()) {
                throw new IllegalArgumentException("One or more player IDs are invalid");
            }
        }

        List<Long> winnerIds = request.winnerPlayerIds() == null ? List.of() : request.winnerPlayerIds();
        if (!winnerIds.isEmpty() && winnerIds.size() != request.timesPlayed()) {
            throw new InvalidWinnerException("Winner count must match times played");
        }

        Map<Long, User> playersById = players.stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        gp.setPlayedAt(request.playedAt());
        gp.setTimesPlayed(request.timesPlayed());
        gp.setDurationMinutes(request.durationMinutes());
        gp.setPlayers(players);
        gp.setNotes(request.notes());

        gp.getWinners().clear();
        for (int i = 0; i < winnerIds.size(); i++) {
            Long winnerId = winnerIds.get(i);
            User winner = null;
            if (winnerId != null) {
                winner = playersById.get(winnerId);
                if (winner == null) {
                    throw new InvalidWinnerException("Winner must be one of the players who played");
                }
            }
            gp.getWinners().add(new GamePlayWinner(gp, i, winner));
        }

        gamePlayRepository.save(gp);

        return GamePlayDto.from(
            gamePlayRepository.findByIdWithAssociations(playId)
                .orElseThrow(() -> new GamePlayNotFoundException(playId))
        );
    }

    public Map<Integer, Integer> getPlaysThisYearByGames(Long userId, List<Integer> gameIds) {
        if (gameIds.isEmpty()) return Map.of();
        int year = LocalDate.now().getYear();
        List<Object[]> rows = gamePlayRepository.sumTimesPlayedByUserAndGamesAndYear(userId, gameIds, year);
        Map<Integer, Integer> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Integer) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    @Transactional
    public void deleteGamePlay(Long userId, Long playId) {
        GamePlay gp = gamePlayRepository.findByIdWithAssociations(playId)
            .orElseThrow(() -> new GamePlayNotFoundException(playId));

        if (!gp.getLogger().getId().equals(userId)) {
            throw new UnauthorizedGamePlayAccessException(playId, userId);
        }

        gamePlayRepository.deleteById(playId);
    }
}
