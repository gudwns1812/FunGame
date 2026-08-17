package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.room.RoomStateInfo;

import java.util.List;

public record RoomStateResponse(
        long version,
        List<GamePlayerResponse> players,
        Long hostMemberId,
        String hostNickname
) {

    public static RoomStateResponse from(RoomStateInfo state) {
        return new RoomStateResponse(
                state.version(),
                GamePlayerResponse.listFrom(state.players()),
                state.host().memberId(),
                state.host().nickname());
    }
}
