package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.invite.AcceptedInvite;

public record AcceptedInviteResponse(
        RoomResponse room,
        int playerSequence
) {

    public static AcceptedInviteResponse from(AcceptedInvite invite) {
        return new AcceptedInviteResponse(RoomResponse.from(invite.room()), invite.playerSequence());
    }
}
