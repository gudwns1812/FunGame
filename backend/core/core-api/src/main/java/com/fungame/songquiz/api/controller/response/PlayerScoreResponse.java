package com.fungame.songquiz.api.controller.response;

import com.fungame.songquiz.domain.member.MemberProfile;
import com.fungame.songquiz.domain.member.MemberProfileCache;
import com.fungame.songquiz.domain.session.PlayerScore;

import java.util.List;
import java.util.Map;

public record PlayerScoreResponse(
        Long memberId,
        String nickname,
        int score,
        int rank
) {

    public static List<PlayerScoreResponse> listFrom(List<PlayerScore> scores, MemberProfileCache profiles) {
        Map<Long, MemberProfile> found = profiles.allOf(scores.stream().map(PlayerScore::memberId).toList());

        return scores.stream()
                .map(score -> new PlayerScoreResponse(
                        score.memberId(), nicknameOf(found, score.memberId()), score.score(), score.rank()))
                .toList();
    }

    private static String nicknameOf(Map<Long, MemberProfile> found, Long memberId) {
        MemberProfile profile = found.get(memberId);
        return profile == null ? null : profile.nickname();
    }
}
