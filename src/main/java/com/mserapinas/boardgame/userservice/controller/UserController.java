package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.annotation.CurrentUser;
import com.mserapinas.boardgame.userservice.dto.request.UpdateUserProfileRequest;
import com.mserapinas.boardgame.userservice.dto.response.UserProfileDto;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
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
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(@CurrentUser Long userId) {
        return userRepository.findById(userId)
            .map(user -> ResponseEntity.ok(UserProfileDto.from(user)))
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateCurrentUserProfile(
            @CurrentUser Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        var updatedUser = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(UserProfileDto.from(updatedUser));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUserAccount(@CurrentUser Long userId) {
        userService.deleteUserAccount(userId);
    }
}
