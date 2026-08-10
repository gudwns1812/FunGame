package com.fungame.songquiz.controller.websocket;

public record RoomMember(Long roomId, String nickname) {

    public String key() {
        return roomId + ":" + nickname;
    }
}
