package com.mserapinas.boardgame.userservice.controller;

import com.mserapinas.boardgame.userservice.dto.request.LoginRequest;
import com.mserapinas.boardgame.userservice.dto.request.RefreshTokenRequest;
import com.mserapinas.boardgame.userservice.dto.request.RegisterRequest;
import com.mserapinas.boardgame.userservice.dto.response.AuthResponse;
import com.mserapinas.boardgame.userservice.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refreshToken(request);
    }

}
