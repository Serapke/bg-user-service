package com.mserapinas.boardgame.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreatePlayerGroupRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @NotEmpty Set<Long> memberIds
) {}
