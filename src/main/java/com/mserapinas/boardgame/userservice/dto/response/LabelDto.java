package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.Label;

public record LabelDto(
    Long id,
    String name
) {
    public static LabelDto from(Label label) {
        return new LabelDto(
            label.getId(),
            label.getName()
        );
    }
}