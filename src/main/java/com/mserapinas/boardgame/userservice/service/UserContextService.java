package com.mserapinas.boardgame.userservice.service;

import com.mserapinas.boardgame.userservice.model.User;
import com.mserapinas.boardgame.userservice.repository.UserRepository;
import com.mserapinas.boardgame.userservice.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserContextService {

    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal);
        }
        
        return Optional.empty();
    }

    public Optional<Long> getCurrentUserId() {
        return getCurrentUserPrincipal().map(UserPrincipal::userId);
    }

    public Optional<String> getCurrentUserEmail() {
        return getCurrentUserPrincipal().map(UserPrincipal::email);
    }

    public Optional<User> getCurrentUser() {
        return getCurrentUserId()
            .flatMap(userRepository::findById);
    }

    public boolean isAuthenticated() {
        return getCurrentUserPrincipal().isPresent();
    }
}