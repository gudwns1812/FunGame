package com.fungame.songquiz.api.controller.response;

import com.fungame.songquiz.domain.member.MemberProfileCache;
import com.fungame.songquiz.domain.room.RoomStateInfo;

import java.util.List;

public record RoomStateResponse(
        long version,
        List<GamePlayerResponse> players,
        Long hostMemberId,
        String hostNickname
) {

    public static RoomStateResponse from(RoomStateInfo state, MemberProfileCache profiles) {
        return new RoomStateResponse(
                state.version(),
                GamePlayerResponse.listFrom(state.players(), profiles),
                state.host().memberId(),
                profiles.of(state.host().memberId()).nickname());
    }
}
