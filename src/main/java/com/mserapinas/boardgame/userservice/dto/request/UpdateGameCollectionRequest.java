package com.mserapinas.boardgame.userservice.dto.request;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateGameCollectionRequest(
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    String notes,
    
    @Size(max = 10, message = "Cannot have more than 10 labels")
    Set<String> labelNames
) {
}