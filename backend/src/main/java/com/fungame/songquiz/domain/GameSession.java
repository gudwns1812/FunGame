package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import java.util.List;

public class GameSession {
    private final Game game;
    private final GameRank rank;

    public GameSession(Game game, List<String> players) {
        this.game = game;
        this.game.setPlayers(players);
        this.rank = new GameRank(players);
    }

    public ActionResult handleAction(GameAction action) {
        return game.handleAction(action);
    }

    public void updatePlayerPoint(String player) {
        rank.updatePoint(player);
    }

    public List<PlayerScore> getPlayerRanks() {
        return rank.getPlayerScores();
    }

    public GameAnswerDto getAnswer() {
        return game.getAnswer();
    }

    public boolean startProcessing() {
        return game.startProcessing();
    }

    public void endRound() {
        game.nextRound();
        game.resetRoundState();
    }

    public GameContentDto getContent() {
        return game.getStatus();
    }

    public boolean isLastRound() {
        return game.isLast();
    }

    public void startRound() {
        game.resetRoundState();
    }

    public int getTotalRound() {
        return game.getTotalRound();
    }

    public boolean hasPlayer(String player) {
        return rank.hasPlayer(player);
    }

    public int getCurrentRound() {
        return game.getCurrentRound();
    }

    public String getHint() {
        return game.getHint();
    }
}
