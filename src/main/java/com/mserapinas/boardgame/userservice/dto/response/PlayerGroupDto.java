package com.mserapinas.boardgame.userservice.dto.response;

import com.mserapinas.boardgame.userservice.model.PlayerGroup;

import java.time.OffsetDateTime;
import java.util.List;

public record PlayerGroupDto(
    Long id,
    String name,
    List<GamePlayDto.PlayerRef> members,
    OffsetDateTime createdAt
) {
    public static PlayerGroupDto from(PlayerGroup pg) {
        List<GamePlayDto.PlayerRef> memberRefs = new java.util.ArrayList<>();
        memberRefs.add(GamePlayDto.PlayerRef.from(pg.getCreator()));
        pg.getMembers().stream()
            .filter(m -> !m.getId().equals(pg.getCreator().getId()))
            .map(GamePlayDto.PlayerRef::from)
            .forEach(memberRefs::add);
        memberRefs.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        return new PlayerGroupDto(pg.getId(), pg.getName(), memberRefs, pg.getCreatedAt());
    }
}
