package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Game;
import com.fungame.songquiz.domain.quiz.GameAnswer;
import com.fungame.songquiz.domain.quiz.GameContentDto;
import com.fungame.songquiz.domain.quiz.GameInfo;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;

import java.util.List;

public class GameSession {
    private final Game game;
    private final GameRank rank;
    private final SkipVotes skipVotes = new SkipVotes();

    public GameSession(Game game, List<GamePlayer> players) {
        this.game = game;
        this.rank = new GameRank(players);
    }

    public Game getGame() {
        return game;
    }

    public ActionResult handleAction(GameAction action) {
        return switch (action.type()) {
            case SUBMIT_ANSWER -> game.submitAnswer(action.memberId(), action.value());
            case SKIP_VOTE -> voteToSkip(action.memberId());
            default -> ActionResult.NO_ACTION;
        };
    }

    private ActionResult voteToSkip(Long memberId) {
        if (!rank.hasPlayer(memberId)) {
            return ActionResult.NO_ACTION;
        }

        skipVotes.add(memberId);
        return skipVotes.isThresholdReached(rank.playerCount())
                ? ActionResult.SKIP_VOTE_SUCCESS
                : ActionResult.ACTION_SUCCESS;
    }

    public void updatePlayerPoint(Long memberId) {
        rank.updatePoint(memberId);
    }

    public String nicknameOf(Long memberId) {
        return rank.nicknameOf(memberId);
    }

    public void removePlayer(Long memberId) {
        rank.deactivate(memberId);
        skipVotes.remove(memberId);
        game.dropPlayer(memberId);
    }

    public boolean canRejoin(Long memberId) {
        return rank.hasParticipant(memberId) && !rank.hasPlayer(memberId);
    }

    public void restorePlayer(GamePlayer player) {
        rank.activate(player);
        game.takeBackPlayer(player);
    }

    public GameInfo getGameInfo() {
        return game.getGameInfo();
    }

    public List<PlayerScore> getPlayerRanks() {
        return rank.getPlayerScores();
    }

    public GameAnswer getAnswer() {
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
        skipVotes.clear();
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
