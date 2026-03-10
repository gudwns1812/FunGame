package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ComputerScienceQuizGame extends AbstractQuizGame {

    private final List<ComputerScienceQuiz> quizs;
    private final AtomicInteger currentIdx = new AtomicInteger(0);

    public ComputerScienceQuizGame(List<ComputerScienceQuiz> quizs) {
        super(null);
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
    protected ActionResult processAnswer(String playerName, String answer) {
        var quiz = quizs.get(currentIdx.get());
        return quiz.isCorrect(answer) ? ActionResult.CORRECT : ActionResult.WRONG;
    }

    @Override
    public GameAnswerDto getAnswer() {
        var quiz = quizs.get(currentIdx.get());

        return GameAnswerDto.from(this, quiz.getAnswer(), quiz.getExplain());
    }

    @Override
    public void nextRound() {
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
}
