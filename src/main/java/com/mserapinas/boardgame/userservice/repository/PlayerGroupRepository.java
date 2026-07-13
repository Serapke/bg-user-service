package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.PlayerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerGroupRepository extends JpaRepository<PlayerGroup, Long> {

    @Query("SELECT DISTINCT pg FROM PlayerGroup pg LEFT JOIN FETCH pg.members WHERE pg.creator.id = :creatorId ORDER BY pg.name")
    List<PlayerGroup> findByCreatorIdWithMembers(@Param("creatorId") Long creatorId);

    @Query("SELECT pg FROM PlayerGroup pg LEFT JOIN FETCH pg.members WHERE pg.id = :id")
    Optional<PlayerGroup> findByIdWithMembers(@Param("id") Long id);
}
