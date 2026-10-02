package com.fungame.songquiz.domain.session;

import java.util.List;

public record ResultRow(
        Long memberId,
        String label,
        int score
) {

    public static ResultRow of(PlayerScore score) {
        return new ResultRow(score.memberId(), null, score.score());
    }

    public static ResultRow labelled(String label, int score) {
        return new ResultRow(null, label, score);
    }

    public static List<ResultRow> listOf(List<PlayerScore> scores) {
        return scores.stream().map(ResultRow::of).toList();
    }
}
