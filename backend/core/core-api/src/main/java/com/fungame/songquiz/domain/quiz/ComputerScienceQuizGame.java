package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ComputerScienceQuizGame extends AbstractQuizGame {

    private static final int ROUND_NOT_STARTED = -1;

    private final List<ComputerScienceQuiz> quizs;
    private final AtomicInteger currentIdx = new AtomicInteger(ROUND_NOT_STARTED);

    public ComputerScienceQuizGame(List<ComputerScienceQuiz> quizs) {
        this.quizs = quizs;
    }

    @Override
    public GameContentDto getStatus() {
        var quiz = quizs.get(currentIdx.get());

        return GameContentDto.from(this, quiz.getField(), quiz.getDifficulty().name(), quiz.getQuestion());
    }

    @Override
    public GameInfo getGameInfo() {
        return new GameInfo(getType().name(), "여러가지 CS 혼합", quizs.size());
    }

    @Override
    public GameType getType() {
        return GameType.CS;
    }

    @Override
    public ActionResult submitAnswer(Long memberId, String answer) {
        int current = currentIdx.get();
        if (current == ROUND_NOT_STARTED) {
            return ActionResult.NO_ACTION;
        }

        var quiz = quizs.get(current);
        return quiz.isCorrect(answer) ? ActionResult.CORRECT : ActionResult.WRONG;
    }

    @Override
    public GameAnswer getAnswer() {
        var quiz = quizs.get(currentIdx.get());

        return new GameAnswer(quiz.getAnswer(), quiz.getExplain());
    }

    @Override
    public void startRound() {
        currentIdx.incrementAndGet();
    }

    @Override
    public boolean isLast() {
        return currentIdx.get() >= quizs.size() - 1;
    }

    @Override
    public int getCurrentRound() {
        return currentIdx.get() + 1;
    }

    @Override
    public int getTotalRound() {
        return quizs.size();
    }

    @Override
    public String getHint() {
        return "";
    }
}
