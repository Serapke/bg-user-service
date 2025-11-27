package com.mserapinas.boardgame.userservice.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Set;

@Entity
@Table(name = "user_board_games")
public class UserBoardGame {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "game_id", nullable = false)
    private Integer gameId;
    
    private String notes;

    @Column(name = "modified_at")
    private OffsetDateTime modifiedAt;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_board_game_labels",
        joinColumns = @JoinColumn(name = "user_board_game_id"),
        inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private Set<Label> labels;
    
    @SuppressWarnings("unused")
    public UserBoardGame() {}
    
    public UserBoardGame(Long userId, Integer gameId, String notes) {
        this.userId = userId;
        this.gameId = gameId;
        this.notes = notes;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getGameId() {
        return gameId;
    }
    
    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public OffsetDateTime getModifiedAt() {
        return modifiedAt;
    }
    
    public void setModifiedAt(OffsetDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
    
    public Set<Label> getLabels() {
        return labels;
    }
    
    public void setLabels(Set<Label> labels) {
        this.labels = labels;
    }
}