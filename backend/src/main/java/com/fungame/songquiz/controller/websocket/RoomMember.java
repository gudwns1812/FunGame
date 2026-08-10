package com.fungame.songquiz.controller.websocket;

/**
 * 어떤 방의 어떤 사람인가. 웹소켓 세션 ID 와 무관한 식별자다.
 * 세션은 재연결마다 새로 발급되지만 이 값은 그대로 유지된다.
 */
public record RoomMember(Long roomId, String nickname) {

    public String key() {
        return roomId + ":" + nickname;
    }
}
