package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.domain.session.GameSession;
import java.util.Optional;

public class RoomTable {

    private final GameRoom room;
    private GameSession game;

    RoomTable(GameRoom room, GameSession game) {
        this.room = room;
        this.game = game;
    }

    public GameRoom room() {
        return room;
    }

    public Optional<GameSession> game() {
        return Optional.ofNullable(game);
    }

    public void startGame(GameSession game) {
        this.game = game;
    }

    public void endGame() {
        this.game = null;
    }
}
