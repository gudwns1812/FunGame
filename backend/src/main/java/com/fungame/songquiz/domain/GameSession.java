package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;

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

    public void removePlayer(String player) {
        game.removePlayer(player);
        rank.deactivate(player);
    }

    /**
     * 이 게임에 참가했다가 이탈한 사람인지 여부. 진행 중인 방의 재입장 허용 판정에 쓰인다.
     */
    public boolean canRejoin(String player) {
        return rank.hasParticipant(player) && !rank.hasPlayer(player);
    }

    public void restorePlayer(String player) {
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
