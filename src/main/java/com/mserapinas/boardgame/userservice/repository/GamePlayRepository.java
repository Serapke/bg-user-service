package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.GamePlay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface GamePlayRepository extends JpaRepository<GamePlay, Long> {

    @Query("SELECT DISTINCT gp FROM GamePlay gp " +
           "LEFT JOIN FETCH gp.players " +
           "WHERE gp.logger.id = :userId AND gp.gameId = :gameId " +
           "ORDER BY gp.playedAt DESC, gp.id DESC")
    List<GamePlay> findByLoggerAndGame(@Param("userId") Long userId, @Param("gameId") Integer gameId);

    @Query("SELECT gp FROM GamePlay gp " +
           "LEFT JOIN FETCH gp.players " +
           "WHERE gp.id = :id")
    Optional<GamePlay> findByIdWithAssociations(@Param("id") Long id);

    @Query("SELECT DISTINCT gp FROM GamePlay gp " +
           "LEFT JOIN FETCH gp.players " +
           "WHERE gp.id IN :ids " +
           "ORDER BY gp.playedAt DESC, gp.id DESC")
    List<GamePlay> findByIds(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT gp FROM GamePlay gp " +
           "LEFT JOIN FETCH gp.players " +
           "WHERE gp.logger.id = :userId " +
           "ORDER BY gp.playedAt DESC, gp.id DESC")
    List<GamePlay> findRecentByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT gp.gameId, SUM(gp.timesPlayed) FROM GamePlay gp " +
           "WHERE gp.logger.id = :userId " +
           "AND gp.gameId IN :gameIds " +
           "AND YEAR(gp.playedAt) = :year " +
           "GROUP BY gp.gameId")
    List<Object[]> sumTimesPlayedByUserAndGamesAndYear(
            @Param("userId") Long userId,
            @Param("gameIds") List<Integer> gameIds,
            @Param("year") int year);
}
