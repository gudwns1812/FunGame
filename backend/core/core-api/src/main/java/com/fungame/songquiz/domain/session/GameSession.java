package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Game;
import com.fungame.songquiz.domain.quiz.GameAnswerDto;
import com.fungame.songquiz.domain.quiz.GameContentDto;
import com.fungame.songquiz.domain.quiz.GameInfo;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;

import java.util.List;

public class GameSession {
    private final Game game;
    private final GameRank rank;

    public GameSession(Game game, List<GamePlayer> players) {
        this.game = game;
        this.game.setPlayers(players);
        this.rank = new GameRank(players);
    }

    public Game getGame() {
        return game;
    }

    public ActionResult handleAction(GameAction action) {
        return game.handleAction(action);
    }

    public void updatePlayerPoint(Long memberId) {
        rank.updatePoint(memberId);
    }

    public String nicknameOf(Long memberId) {
        return rank.nicknameOf(memberId);
    }

    public void removePlayer(Long memberId) {
        game.removePlayer(memberId);
        rank.deactivate(memberId);
    }

    public boolean canRejoin(Long memberId) {
        return rank.hasParticipant(memberId) && !rank.hasPlayer(memberId);
    }

    public void restorePlayer(GamePlayer player) {
        game.restorePlayer(player);
        rank.activate(player);
    }

    public GameInfo getGameInfo() {
        return game.getGameInfo();
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

    public GameContentDto getContent() {
        return game.getStatus();
    }

    public boolean isLastRound() {
        return game.isLast();
    }

    public void startRound() {
        game.startRound();
        game.resetRoundState();
    }

    public int getTotalRound() {
        return game.getTotalRound();
    }

    public boolean hasPlayer(Long memberId) {
        return rank.hasPlayer(memberId);
    }

    public int getCurrentRound() {
        return game.getCurrentRound();
    }

    public String getHint() {
        return game.getHint();
    }
}
