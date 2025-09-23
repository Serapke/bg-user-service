package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.dto.request.UpdateUserProfileRequest;
import com.mserapinas.boardgame.userservice.dto.response.UserProfileDto;
import com.mserapinas.boardgame.userservice.service.UserContextService;
import com.mserapinas.boardgame.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserContextService userContextService;

    public UserController(UserService userService, UserContextService userContextService) {
        this.userService = userService;
        this.userContextService = userContextService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser() {
        return userContextService.getCurrentUser()
            .map(user -> ResponseEntity.ok(UserProfileDto.from(user)))
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateCurrentUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        var updatedUser = userService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(UserProfileDto.from(updatedUser));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUserAccount() {
        userService.deleteCurrentUserAccount();
    }
}
