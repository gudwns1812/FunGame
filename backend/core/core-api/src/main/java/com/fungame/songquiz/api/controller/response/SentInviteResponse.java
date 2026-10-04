package com.fungame.songquiz.api.controller.response;

import com.fungame.songquiz.domain.invite.SentInvite;

public record SentInviteResponse(long expiresInSeconds) {

    public static SentInviteResponse from(SentInvite invite) {
        return new SentInviteResponse(invite.expiresInSeconds());
    }
}
