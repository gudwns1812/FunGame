package com.fungame.songquiz.controller.room;

import com.fungame.songquiz.domain.room.GamePlayer;

public record RoomMember(Long roomId, Long memberId, String nickname) {

    public String key() {
        return roomId + ":" + memberId;
    }

    public GamePlayer toPlayer() {
        return GamePlayer.createNewPlayer(memberId, nickname);
    }
}
