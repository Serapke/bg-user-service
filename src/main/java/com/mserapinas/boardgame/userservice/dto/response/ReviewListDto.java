package com.mserapinas.boardgame.userservice.dto.response;

import java.util.List;

public record ReviewListDto(
    List<ReviewDto> reviews,
    Long totalCount,
    Double averageRating
) {
}