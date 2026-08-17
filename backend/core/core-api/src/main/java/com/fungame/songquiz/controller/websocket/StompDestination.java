package com.fungame.songquiz.controller.websocket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StompDestination {

    /** 전체가 함께 받는 destination */
    public static final String BROKER_PREFIX = "/topic";

    /** 받는 사람마다 내용이 다른 destination. 클라이언트는 /user 를 앞에 붙여 구독한다 */
    public static final String USER_BROKER_PREFIX = "/queue";

    public static final String APPLICATION_PREFIX = "/app";

    /** 방 목록 갱신 */
    public static final String LOBBY = BROKER_PREFIX + "/lobby";

    /** 접속자 목록. 뷰어 자신은 빠지므로 사람마다 내용이 다르다 */
    public static final String PRESENCE = USER_BROKER_PREFIX + "/presence";

    /** 방 초대 */
    public static final String INVITE = USER_BROKER_PREFIX + "/invite";

    private static final String ROOM_PREFIX = BROKER_PREFIX + "/room/";

    private StompDestination() {
    }

    public static String room(Long roomId) {
        return ROOM_PREFIX + roomId;
    }

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
