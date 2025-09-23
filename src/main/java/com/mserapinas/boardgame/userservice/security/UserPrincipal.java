package com.mserapinas.boardgame.userservice.security;

import java.security.Principal;

public record UserPrincipal(
    Long userId,
    String email
) implements Principal {
    
    @Override
    public String getName() {
        return email;
    }
}