package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.Friendship;
import com.mserapinas.boardgame.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * Get accepted friends (bidirectional check - both rows exist)
     * Uses JOIN FETCH to eagerly load friend User objects in a single query
     */
    @Query("""
        SELECT f FROM Friendship f JOIN FETCH f.friend
        WHERE f.user.id = :userId
        AND EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = f.friend.id AND f2.friend.id = :userId
        )
        ORDER BY f.createdAt DESC
        """)
    List<Friendship> getFriends(@Param("userId") Long userId);

    /**
     * Get friends of friends (only accepted friendships)
     * Returns users who are friends of my friends but NOT my direct friends
     */
    @Query("""
        SELECT DISTINCT f2.friend FROM Friendship f1
        JOIN Friendship f2 ON f1.friend.id = f2.user.id
        WHERE f1.user.id = :userId
        AND f2.friend.id != :userId
        AND EXISTS (SELECT 1 FROM Friendship f1b WHERE f1b.user.id = f1.friend.id AND f1b.friend.id = :userId)
        AND EXISTS (SELECT 1 FROM Friendship f2b WHERE f2b.user.id = f2.friend.id AND f2b.friend.id = f2.user.id)
        AND f2.friend.id NOT IN (
            SELECT f3.friend.id FROM Friendship f3
            WHERE f3.user.id = :userId
            AND EXISTS (SELECT 1 FROM Friendship f3b WHERE f3b.user.id = f3.friend.id AND f3b.friend.id = :userId)
        )
        ORDER BY f2.friend.name
        """)
    List<User> getFriendsOfFriends(@Param("userId") Long userId);

    /**
     * Check if bidirectional friendship exists (both directions)
     */
    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM Friendship f
        WHERE f.user.id = :userId AND f.friend.id = :friendId
        AND EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = :friendId AND f2.friend.id = :userId
        )
        """)
    boolean areFriends(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * Delete friendship in both directions (encapsulates bidirectional implementation)
     */
    @Modifying
    @Query("DELETE FROM Friendship f WHERE (f.user.id = :userId AND f.friend.id = :friendId) OR (f.user.id = :friendId AND f.friend.id = :userId)")
    void deleteFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * Count accepted friends
     */
    @Query("""
        SELECT COUNT(f) FROM Friendship f
        WHERE f.user.id = :userId
        AND EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = f.friend.id AND f2.friend.id = :userId
        )
        """)
    Long countFriends(@Param("userId") Long userId);
}