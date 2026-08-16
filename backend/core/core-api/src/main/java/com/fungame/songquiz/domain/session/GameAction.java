package com.fungame.songquiz.domain.session;

import com.fungame.songquiz.enums.ActionType;

public record GameAction(Long memberId, ActionType type, String value) {
    public static GameAction submitAnswer(Long memberId, String answer) {
        return new GameAction(memberId, ActionType.SUBMIT_ANSWER, answer);
    }

    public static GameAction skipVote(Long memberId) {
        return new GameAction(memberId, ActionType.SKIP_VOTE, null);
    }
}
