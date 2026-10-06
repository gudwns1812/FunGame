package com.fungame.songquiz.api.controller.response;

import com.fungame.songquiz.domain.invite.AcceptedInvite;
import com.fungame.songquiz.domain.member.MemberProfileCache;

public record AcceptedInviteResponse(
        RoomResponse room,
        int playerSequence
) {

    public static AcceptedInviteResponse from(AcceptedInvite invite, MemberProfileCache profiles) {
        return new AcceptedInviteResponse(RoomResponse.from(invite.room(), profiles), invite.playerSequence());
    }
}
