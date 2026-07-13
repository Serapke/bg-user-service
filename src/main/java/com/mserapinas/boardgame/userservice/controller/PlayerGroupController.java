package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.annotation.CurrentUser;
import com.mserapinas.boardgame.userservice.dto.request.CreatePlayerGroupRequest;
import com.mserapinas.boardgame.userservice.dto.request.UpdatePlayerGroupRequest;
import com.mserapinas.boardgame.userservice.dto.response.PlayerGroupDto;
import com.mserapinas.boardgame.userservice.service.PlayerGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/player-groups")
public class PlayerGroupController {

    private final PlayerGroupService playerGroupService;

    public PlayerGroupController(PlayerGroupService playerGroupService) {
        this.playerGroupService = playerGroupService;
    }

    @GetMapping
    public ResponseEntity<List<PlayerGroupDto>> listGroups(@CurrentUser Long userId) {
        return ResponseEntity.ok(playerGroupService.listGroups(userId));
    }

    @PostMapping
    public ResponseEntity<PlayerGroupDto> createGroup(
            @CurrentUser Long userId,
            @Valid @RequestBody CreatePlayerGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerGroupService.createGroup(userId, request));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<PlayerGroupDto> updateGroup(
            @CurrentUser Long userId,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdatePlayerGroupRequest request) {
        return ResponseEntity.ok(playerGroupService.updateGroup(userId, groupId, request));
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(
            @CurrentUser Long userId,
            @PathVariable Long groupId) {
        playerGroupService.deleteGroup(userId, groupId);
    }
}
