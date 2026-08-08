package com.fungame.songquiz.controller.websocket;

import lombok.extern.slf4j.Slf4j;

/**
 * STOMP 목적지 규약의 단일 출처.
 * <p>
 * 프론트엔드가 {@code /subscribe/room/{roomId}} 를 그대로 구독하므로 이 값은
 * 배포 환경별 설정이 아니라 클라이언트와 맞춘 통신 규약이다.
 * 설정 파일로 빼면 서버만 목적지가 바뀌어 아무도 받지 못하는 곳으로 발행하게 되고,
 * 컴파일이나 테스트로는 잡히지 않는다. 그래서 코드 상수로 고정한다.
 */
@Slf4j
public final class StompDestination {

    /** 브로커가 구독을 받는 접두사. WebSocketConfig 의 enableSimpleBroker 와 같아야 한다. */
    public static final String BROKER_PREFIX = "/subscribe";

    /** 클라이언트가 서버로 보낼 때 쓰는 접두사. @MessageMapping 경로 앞에 붙는다. */
    public static final String APPLICATION_PREFIX = "/publish";

    private static final String ROOM_PREFIX = BROKER_PREFIX + "/room/";

    private StompDestination() {
    }

    /** 방 단위 브로드캐스트 목적지. */
    public static String room(Long roomId) {
        return ROOM_PREFIX + roomId;
    }

    /**
     * 구독 목적지에서 roomId 를 뽑는다.
     *
     * @return 방 구독이 아니거나 roomId 형식이 잘못됐으면 null
     */
    public static Long roomIdOf(String destination) {
        if (destination == null || !destination.startsWith(ROOM_PREFIX)) {
            return null;
        }

        String rawRoomId = destination.substring(ROOM_PREFIX.length());
        try {
            return Long.parseLong(rawRoomId);
        } catch (NumberFormatException e) {
            log.warn("Invalid room id in subscribe destination: {}", destination);
            return null;
        }
    }
}
