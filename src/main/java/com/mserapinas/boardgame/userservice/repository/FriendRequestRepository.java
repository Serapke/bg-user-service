package com.mserapinas.boardgame.userservice.repository;

import com.mserapinas.boardgame.userservice.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<Friendship, Long> {

    /**
     * Get pending outgoing requests (I sent, not accepted yet)
     */
    @Query("""
        SELECT f FROM Friendship f JOIN FETCH f.friend
        WHERE f.user.id = :userId
        AND NOT EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = f.friend.id AND f2.friend.id = :userId
        )
        ORDER BY f.createdAt DESC
        """)
    List<Friendship> getOutgoingRequests(@Param("userId") Long userId);

    /**
     * Get pending incoming requests (others sent to me, I haven't accepted)
     */
    @Query("""
        SELECT f FROM Friendship f JOIN FETCH f.user
        WHERE f.friend.id = :userId
        AND NOT EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = :userId AND f2.friend.id = f.user.id
        )
        ORDER BY f.createdAt DESC
        """)
    List<Friendship> getIncomingRequests(@Param("userId") Long userId);

    /**
     * Check if pending request exists (one direction, not accepted)
     */
    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM Friendship f
        WHERE f.user.id = :userId AND f.friend.id = :friendId
        AND NOT EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = :friendId AND f2.friend.id = :userId
        )
        """)
    boolean hasPendingRequest(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * Find specific pending request
     */
    @Query("""
        SELECT f FROM Friendship f
        WHERE f.user.id = :userId AND f.friend.id = :friendId
        AND NOT EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = :friendId AND f2.friend.id = :userId
        )
        """)
    Optional<Friendship> findPendingRequest(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * Check if any relationship exists (one direction, pending or accepted)
     */
    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM Friendship f
        WHERE f.user.id = :userId AND f.friend.id = :friendId
        """)
    boolean existsByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * Delete a single friend request (one direction only)
     */
    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.user.id = :userId AND f.friend.id = :friendId")
    void deleteByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * Count pending outgoing requests
     */
    @Query("""
        SELECT COUNT(f) FROM Friendship f
        WHERE f.user.id = :userId
        AND NOT EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = f.friend.id AND f2.friend.id = :userId
        )
        """)
    Long countOutgoingRequests(@Param("userId") Long userId);

    /**
     * Count pending incoming requests
     */
    @Query("""
        SELECT COUNT(f) FROM Friendship f
        WHERE f.friend.id = :userId
        AND NOT EXISTS (
            SELECT 1 FROM Friendship f2
            WHERE f2.user.id = :userId AND f2.friend.id = f.user.id
        )
        """)
    Long countIncomingRequests(@Param("userId") Long userId);
}