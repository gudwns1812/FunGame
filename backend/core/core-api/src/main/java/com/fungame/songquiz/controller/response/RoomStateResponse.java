package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.member.MemberProfiles;
import com.fungame.songquiz.domain.room.RoomStateInfo;

import java.util.List;

public record RoomStateResponse(
        long version,
        List<GamePlayerResponse> players,
        Long hostMemberId,
        String hostNickname
) {

    public static RoomStateResponse from(RoomStateInfo state, MemberProfiles profiles) {
        return new RoomStateResponse(
                state.version(),
                GamePlayerResponse.listFrom(state.players(), profiles),
                state.host().memberId(),
                profiles.of(state.host().memberId()).nickname());
    }
}
