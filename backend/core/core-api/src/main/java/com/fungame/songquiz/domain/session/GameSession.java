package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.domain.quiz.Quiz;
import com.fungame.songquiz.domain.quiz.QuizAnswer;
import com.fungame.songquiz.domain.quiz.QuizContent;
import com.fungame.songquiz.domain.quiz.QuizInfo;
import com.fungame.songquiz.domain.room.GamePlayer;
import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;

import java.util.List;

public class GameSession {
    private final Quiz quiz;
    private final GameRank rank;
    private final SkipVotes skipVotes = new SkipVotes();

    public GameSession(Quiz quiz, List<GamePlayer> players) {
        this.quiz = quiz;
        this.rank = new GameRank(players);
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public ActionResult handleAction(GameAction action) {
        return switch (action.type()) {
            case SUBMIT_ANSWER -> quiz.submitAnswer(action.memberId(), action.value());
            case SKIP_VOTE -> voteToSkip(action.memberId());
            default -> ActionResult.NO_ACTION;
        };
    }

    private ActionResult voteToSkip(Long memberId) {
        if (!canVoteToSkip(memberId)) {
            return ActionResult.NO_ACTION;
        }

        skipVotes.add(memberId);
        return skipVotes.isThresholdReached(rank.playerCount())
                ? ActionResult.SKIP_VOTE_SUCCESS
                : ActionResult.ACTION_SUCCESS;
    }

    private boolean canVoteToSkip(Long memberId) {
        return quiz.isRoundStarted() && rank.hasPlayer(memberId);
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
        quiz.dropPlayer(memberId);
    }

    public boolean canRejoin(Long memberId) {
        return rank.hasParticipant(memberId) && !rank.hasPlayer(memberId);
    }

    public void restorePlayer(GamePlayer player) {
        rank.activate(player);
        quiz.takeBackPlayer(player);
    }

    public QuizInfo getQuizInfo() {
        return quiz.getQuizInfo();
    }

    public List<PlayerScore> getPlayerRanks() {
        return rank.getPlayerScores();
    }

    public QuizAnswer getAnswer() {
        return quiz.getAnswer();
    }

    public boolean startProcessing() {
        return quiz.startProcessing();
    }

    public QuizContent getContent() {
        return quiz.getStatus();
    }

    public boolean isLastRound() {
        return quiz.isLast();
    }

    public void startRound() {
        quiz.startRound();
        quiz.resetRoundState();
        skipVotes.clear();
    }

    public int getTotalRound() {
        return quiz.getTotalRound();
    }

    public boolean hasPlayer(Long memberId) {
        return rank.hasPlayer(memberId);
    }

    public int getCurrentRound() {
        return quiz.getCurrentRound();
    }

    public String getHint() {
        return quiz.getHint();
    }

    public GameType getGameType() {
        return quiz.getType();
    }

    public boolean isRoundStarted() {
        return quiz.isRoundStarted();
    }

    public Long getCurrentContentId() {
        return quiz.getCurrentContentId();
    }
}
