package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.UserBoardGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBoardGameRepository extends JpaRepository<UserBoardGame, Long> {
    
    @Query("SELECT ubg FROM UserBoardGame ubg LEFT JOIN FETCH ubg.labels WHERE ubg.userId = :userId ORDER BY ubg.modifiedAt DESC")
    List<UserBoardGame> findByUserIdWithLabels(@Param("userId") Long userId);
    
    boolean existsByUserIdAndGameId(Long userId, Integer gameId);
    
    void deleteByUserIdAndGameId(Long userId, Integer gameId);
    
    @Query("SELECT ubg FROM UserBoardGame ubg LEFT JOIN FETCH ubg.labels WHERE ubg.userId = :userId AND ubg.gameId = :gameId")
    Optional<UserBoardGame> findByUserIdAndGameIdWithLabels(@Param("userId") Long userId, @Param("gameId") Integer gameId);
}