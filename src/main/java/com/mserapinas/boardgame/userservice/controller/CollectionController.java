package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.annotation.CurrentUser;
import com.mserapinas.boardgame.userservice.dto.request.AddGameToCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdateGameCollectionRequest;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionDto;
import com.mserapinas.boardgame.userservice.dto.response.GameCollectionItemDto;
import com.mserapinas.boardgame.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/collections")
public class CollectionController {

    private final UserService userService;

    public CollectionController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<GameCollectionDto> getCurrentUserGameCollection(@CurrentUser Long userId) {
        GameCollectionDto collection = userService.getUserGameCollection(userId);
        return ResponseEntity.ok(collection);
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<GameCollectionItemDto> addGameToCollection(
            @CurrentUser Long userId,
            @Valid @RequestBody AddGameToCollectionRequest request) {
        GameCollectionItemDto addedGame = userService.addGameToCollection(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(addedGame);
    }

    @PutMapping("/games/{gameId}")
    public ResponseEntity<GameCollectionItemDto> updateGameInCollection(
            @CurrentUser Long userId,
            @PathVariable Integer gameId,
            @Valid @RequestBody UpdateGameCollectionRequest request) {
        GameCollectionItemDto updatedGame = userService.updateGameInCollection(userId, gameId, request);
        return ResponseEntity.ok(updatedGame);
    }

    @DeleteMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGameFromCollection(
            @CurrentUser Long userId,
            @PathVariable Integer gameId) {
        userService.deleteGameFromCollection(userId, gameId);
    }
}