package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameSkipInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameSession {
    private final Game game;
    private final GameRank rank;
    private final AtomicBoolean isRoundEnd;
    private final Map<String, Boolean> skipVote;

    public GameSession(Game game, List<String> players) {
        this.game = game;
        this.rank = new GameRank(players);
        isRoundEnd = new AtomicBoolean(false);
        this.skipVote = new ConcurrentHashMap<>();
        initSkipVote(players);
    }

    private void initSkipVote(List<String> players) {
        players.forEach(player -> skipVote.put(player, false));
    }

    public boolean isCorrectAnswer(String answer) {
        return game.isCorrect(answer);
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
        return isRoundEnd.compareAndSet(false, true);
    }

    public void endRound() {
        game.nextRound();
        skipVote.replaceAll((userId, voted) -> false);
    }

    public GameContentDto getContent() {
        return game.getContent();
    }

    public boolean isLastRound() {
        return game.isLast();
    }

    public void startRound() {
        isRoundEnd.set(false);
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

    public GameSkipInfo voteSkip(String player) {
        if (!hasPlayer(player)) {
            return getSkipInfo();
        }
        skipVote.put(player, true);
        return getSkipInfo();
    }

    public GameSkipInfo getSkipInfo() {
        int count = (int) skipVote.values().stream()
                .filter(value -> value)
                .count();

        // 1명인 경우 스킵 불가 방지 (최소 1명 이상 투표 시 스킵 또는 별도 규칙 적용 가능)
        // 현재 로직: 과반수 또는 특정 인원수 기준 (기존 로직 유지하며 캡슐화)
        int required = skipVote.size() - 1;
        if (required <= 0) required = 1; // 1명인 방 고려

        boolean isSkip = count >= required;
        return new GameSkipInfo(isSkip, count, required);
    }
}
