package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.gameId = :gameId ORDER BY r.createdAt DESC")
    List<Review> findByGameIdWithUser(@Param("gameId") Integer gameId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.id = :id")
    Optional<Review> findByIdWithUser(@Param("id") Long id);

    boolean existsByUserIdAndGameId(Long userId, Integer gameId);
}