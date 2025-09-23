package com.mserapinas.boardgame.userservice.controller;

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
@RequestMapping("/api/v1/collection")
public class CollectionController {

    private final UserService userService;

    public CollectionController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<GameCollectionDto> getCurrentUserGameCollection() {
        GameCollectionDto collection = userService.getCurrentUserGameCollection();
        return ResponseEntity.ok(collection);
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<GameCollectionItemDto> addGameToCollection(@Valid @RequestBody AddGameToCollectionRequest request) {
        GameCollectionItemDto addedGame = userService.addGameToCollection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(addedGame);
    }

    @PutMapping("/games/{gameId}")
    public ResponseEntity<GameCollectionItemDto> updateGameInCollection(
            @PathVariable Integer gameId, 
            @Valid @RequestBody UpdateGameCollectionRequest request) {
        GameCollectionItemDto updatedGame = userService.updateGameInCollection(gameId, request);
        return ResponseEntity.ok(updatedGame);
    }

    @DeleteMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGameFromCollection(@PathVariable Integer gameId) {
        userService.deleteGameFromCollection(gameId);
    }
}