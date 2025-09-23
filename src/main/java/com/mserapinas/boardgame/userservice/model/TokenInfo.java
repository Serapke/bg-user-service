package com.mserapinas.boardgame.userservice.model;

import java.time.OffsetDateTime;

public record TokenInfo(
    String token,
    OffsetDateTime expiresAt
) {
}