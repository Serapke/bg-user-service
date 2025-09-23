package com.mserapinas.boardgame.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AddGameToCollectionRequest(
    @NotNull(message = "Game ID is required")
    @Positive(message = "Game ID must be positive")
    Integer gameId,
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    String notes,
    
    @Size(max = 10, message = "Cannot have more than 10 labels")
    Set<String> labelNames
) {
}