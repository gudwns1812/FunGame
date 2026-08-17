package com.fungame.songquiz.controller.room;

public record RoomMember(Long roomId, Long memberId, String nickname) {

    public RoomMemberKey key() {
        return new RoomMemberKey(roomId, memberId);
    }
}
