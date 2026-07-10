package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.annotation.CurrentUser;
import com.mserapinas.boardgame.userservice.dto.request.CreateGamePlayRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateGamePlayRequest;
import com.mserapinas.boardgame.userservice.dto.response.GamePlayDto;
import com.mserapinas.boardgame.userservice.service.GamePlayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/plays")
public class GamePlayController {

    private final GamePlayService gamePlayService;

    public GamePlayController(GamePlayService gamePlayService) {
        this.gamePlayService = gamePlayService;
    }

    @PostMapping
    public ResponseEntity<GamePlayDto> createGamePlay(
            @CurrentUser Long userId,
            @Valid @RequestBody CreateGamePlayRequest request) {
        GamePlayDto play = gamePlayService.createGamePlay(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(play);
    }

    @GetMapping
    public ResponseEntity<List<GamePlayDto>> getPlaysForGame(
            @CurrentUser Long userId,
            @RequestParam Integer gameId) {
        return ResponseEntity.ok(gamePlayService.getPlaysForGame(userId, gameId));
    }

    @GetMapping("/recent-games")
    public ResponseEntity<List<GamePlayDto>> getRecentGames(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(gamePlayService.getRecentGames(userId, limit));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<GamePlayDto>> getPlaysByIds(
            @RequestParam List<Long> ids) {
        return ResponseEntity.ok(gamePlayService.getPlaysByIds(ids));
    }

    @PutMapping("/{playId}")
    public ResponseEntity<GamePlayDto> updateGamePlay(
            @CurrentUser Long userId,
            @PathVariable Long playId,
            @Valid @RequestBody UpdateGamePlayRequest request) {
        return ResponseEntity.ok(gamePlayService.updateGamePlay(userId, playId, request));
    }

    @DeleteMapping("/{playId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGamePlay(
            @CurrentUser Long userId,
            @PathVariable Long playId) {
        gamePlayService.deleteGamePlay(userId, playId);
    }
}
