package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.ActionResult;
import com.fungame.songquiz.enums.GameType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CsQuiz extends AbstractQuiz {

    private static final int ROUND_NOT_STARTED = -1;

    private final List<CsQuestion> quizs;
    private final AtomicInteger currentIdx = new AtomicInteger(ROUND_NOT_STARTED);

    public CsQuiz(List<CsQuestion> quizs) {
        this.quizs = quizs;
    }

    @Override
    public QuizContent getStatus() {
        var quiz = quizs.get(currentIdx.get());
        String difficulty = quiz.getDifficulty().name();

        String description = "분류: " + quiz.getField()
                + ", 난이도: " + difficulty
                + ", 질문: " + quiz.getQuestion();

        return new QuizContent(description, List.of(quiz.getField(), difficulty, quiz.getQuestion()));
    }

    @Override
    public QuizInfo getQuizInfo() {
        return new QuizInfo(getType().name(), "여러가지 CS 혼합", quizs.size());
    }

    @Override
    public GameType getType() {
        return GameType.CS;
    }

    @Override
    public ActionResult submitAnswer(Long memberId, String answer) {
        if (!isRoundStarted()) {
            return ActionResult.NO_ACTION;
        }

        var quiz = quizs.get(currentIdx.get());
        return quiz.isCorrect(answer) ? ActionResult.CORRECT : ActionResult.WRONG;
    }

    @Override
    public QuizAnswer getAnswer() {
        var quiz = quizs.get(currentIdx.get());

        return new QuizAnswer(quiz.getAnswer(), quiz.getExplain());
    }

    @Override
    public void startRound() {
        currentIdx.incrementAndGet();
    }

    @Override
    public boolean isRoundStarted() {
        return currentIdx.get() != ROUND_NOT_STARTED;
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
