package com.mserapinas.boardgame.userservice.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_friendships_user_friend",
        columnNames = {"user_id", "friend_id"}
    )
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Friendship() {}

    public Friendship(User user, User friend) {
        this.user = user;
        this.friend = friend;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getFriend() {
        return friend;
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public Long getFriendId() {
        return friend != null ? friend.getId() : null;
    }
}