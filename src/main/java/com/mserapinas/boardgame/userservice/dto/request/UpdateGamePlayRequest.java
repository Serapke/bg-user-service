package com.mserapinas.boardgame.userservice.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record UpdateGamePlayRequest(
    @NotNull(message = "Played at is required")
    LocalDate playedAt,

    @NotNull(message = "Times played is required")
    @Min(value = 1, message = "Times played must be at least 1")
    Integer timesPlayed,

    @Positive(message = "Duration must be positive")
    Integer durationMinutes,

    Set<Long> playerIds,

    List<List<Long>> winnerPlayerIds,

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    String notes
) {
}
