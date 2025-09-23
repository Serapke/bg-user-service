package com.mserapinas.boardgame.userservice.dto.response;

import java.time.OffsetDateTime;

public record AuthResponse(
    String token,
    String refreshToken,
    OffsetDateTime expiresAt,
    OffsetDateTime refreshExpiresAt,
    UserResponse user
) {
}
