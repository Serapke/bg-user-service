package com.mserapinas.boardgame.userservice.dto.request;

import com.mserapinas.boardgame.userservice.model.CollectionVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,
    @NotNull(message = "Collection visibility is required")
    CollectionVisibility collectionVisibility
) {
}