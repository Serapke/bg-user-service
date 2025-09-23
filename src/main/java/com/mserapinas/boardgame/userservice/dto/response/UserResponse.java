package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.User;

public record UserResponse(
    Long id,
    String name
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName()
        );
    }
}
