package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.domain.session.GameAction;
import com.fungame.songquiz.enums.ActionType;

public record GameActionRequest(ActionType type, String value) {

    public GameAction toAction(Long memberId) {
        return new GameAction(memberId, type, value);
    }
}
