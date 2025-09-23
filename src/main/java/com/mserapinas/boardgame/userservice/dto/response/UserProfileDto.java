package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.User;

import java.time.OffsetDateTime;

public record UserProfileDto(
    Long id,
    String email,
    String name,
    OffsetDateTime createdAt
) {
    public static UserProfileDto from(User user) {
        return new UserProfileDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getCreatedAt()
        );
    }
}