package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.session.PlayerScore;

import java.util.List;

public record PlayerScoreResponse(
        Long memberId,
        String nickname,
        int score
) {

    public static PlayerScoreResponse from(PlayerScore score) {
        return new PlayerScoreResponse(score.memberId(), score.nickname(), score.score());
    }

    public static List<PlayerScoreResponse> listFrom(List<PlayerScore> scores) {
        return scores.stream()
                .map(PlayerScoreResponse::from)
                .toList();
    }
}
