package com.fungame.songquiz.controller.request;

import com.fungame.songquiz.enums.ActionType;
import com.fungame.songquiz.domain.GameAction;

public record GameActionRequest(ActionType type, String value) {

    public GameAction toAction(Long memberId) {
        return new GameAction(memberId, type, value);
    }
}
