package com.mserapinas.boardgame.userservice.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "game_plays")
public class GamePlay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User logger;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "game_play_players",
        joinColumns = @JoinColumn(name = "game_play_id"),
        inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private Set<User> players = new HashSet<>();

    @OneToMany(mappedBy = "gamePlay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GamePlayWinner> winners = new ArrayList<>();

    @Column(name = "game_id", nullable = false)
    private Integer gameId;

    @Column(name = "played_at", nullable = false)
    private LocalDate playedAt;

    @Column(name = "times_played", nullable = false)
    private Integer timesPlayed;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public GamePlay() {}

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public List<GamePlayWinner> getWinnersOrdered() {
        return winners.stream()
            .sorted(Comparator.comparingInt(GamePlayWinner::getGameIndex))
            .toList();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getLogger() { return logger; }
    public void setLogger(User logger) { this.logger = logger; }
    public Long getLoggerId() { return logger != null ? logger.getId() : null; }

    public Set<User> getPlayers() { return players; }
    public void setPlayers(Set<User> players) { this.players = players; }

    public List<GamePlayWinner> getWinners() { return winners; }
    public void setWinners(List<GamePlayWinner> winners) { this.winners = winners; }

    public Integer getGameId() { return gameId; }
    public void setGameId(Integer gameId) { this.gameId = gameId; }

    public LocalDate getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDate playedAt) { this.playedAt = playedAt; }

    public Integer getTimesPlayed() { return timesPlayed; }
    public void setTimesPlayed(Integer timesPlayed) { this.timesPlayed = timesPlayed; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
