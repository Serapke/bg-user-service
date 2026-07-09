package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.annotation.CurrentUser;
import com.mserapinas.boardgame.userservice.dto.request.CreateGamePlayRequest;
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

    @GetMapping("/batch")
    public ResponseEntity<List<GamePlayDto>> getPlaysByIds(
            @RequestParam List<Long> ids) {
        return ResponseEntity.ok(gamePlayService.getPlaysByIds(ids));
    }
}
