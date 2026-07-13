package com.mserapinas.boardgame.userservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "game_play_winners")
public class GamePlayWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_play_id", nullable = false)
    private GamePlay gamePlay;

    @Column(name = "game_index", nullable = false)
    private Integer gameIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_player_id")
    private User winner;

    public GamePlayWinner() {}

    public GamePlayWinner(GamePlay gamePlay, int gameIndex, User winner) {
        this.gamePlay = gamePlay;
        this.gameIndex = gameIndex;
        this.winner = winner;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GamePlay getGamePlay() { return gamePlay; }
    public void setGamePlay(GamePlay gamePlay) { this.gamePlay = gamePlay; }

    public int getGameIndex() { return gameIndex != null ? gameIndex : 0; }
    public void setGameIndex(Integer gameIndex) { this.gameIndex = gameIndex; }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
}
