package com.fungame.songquiz.controller.room;

import com.fungame.songquiz.domain.room.GamePlayer;

public record RoomMember(Long roomId, GamePlayer player) {

    public static RoomMember of(Long roomId, Long memberId, String nickname) {
        return new RoomMember(roomId, GamePlayer.createNewPlayer(memberId, nickname));
    }

    public Long memberId() {
        return player.memberId();
    }

    public String nickname() {
        return player.nickname();
    }

    public String key() {
        return roomId + ":" + memberId();
    }
}
