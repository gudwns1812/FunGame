package com.fungame.songquiz.domain.invite;

import com.fungame.songquiz.domain.room.RoomInfo;

public record AcceptedInvite(
        RoomInfo room,
        int playerSequence
) {
}
