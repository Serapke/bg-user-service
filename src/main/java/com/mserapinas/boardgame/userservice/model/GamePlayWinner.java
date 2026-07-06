package com.mserapinas.boardgame.userservice.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "game_play_winners")
public class GamePlayWinner {

    @EmbeddedId
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gamePlayId")
    @JoinColumn(name = "game_play_id")
    private GamePlay gamePlay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_player_id")
    private User winner;

    public GamePlayWinner() {}

    public GamePlayWinner(GamePlay gamePlay, int gameIndex, User winner) {
        this.gamePlay = gamePlay;
        this.winner = winner;
        this.id = new Id(gamePlay.getId(), gameIndex);
    }

    public Id getId() { return id; }
    public void setId(Id id) { this.id = id; }

    public GamePlay getGamePlay() { return gamePlay; }
    public void setGamePlay(GamePlay gamePlay) { this.gamePlay = gamePlay; }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }

    public int getGameIndex() { return id != null ? id.gameIndex : 0; }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "game_play_id")
        private Long gamePlayId;

        @Column(name = "game_index")
        private Integer gameIndex;

        public Id() {}

        public Id(Long gamePlayId, Integer gameIndex) {
            this.gamePlayId = gamePlayId;
            this.gameIndex = gameIndex;
        }

        public Long getGamePlayId() { return gamePlayId; }
        public Integer getGameIndex() { return gameIndex; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id other)) return false;
            return Objects.equals(gamePlayId, other.gamePlayId)
                && Objects.equals(gameIndex, other.gameIndex);
        }

        @Override
        public int hashCode() { return Objects.hash(gamePlayId, gameIndex); }
    }
}
