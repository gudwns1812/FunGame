package com.fungame.songquiz.domain;

public record RoomMember(Long roomId, Long memberId, String nickname) {

    public String key() {
        return roomId + ":" + nickname;
    }
}
