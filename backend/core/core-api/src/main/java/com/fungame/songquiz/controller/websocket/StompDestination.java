package com.fungame.songquiz.controller.websocket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StompDestination {

    public static final String BROKER_PREFIX = "/topic";

    public static final String USER_BROKER_PREFIX = "/queue";

    public static final String APPLICATION_PREFIX = "/app";

    public static final String LOBBY = BROKER_PREFIX + "/lobby";

    public static final String PRESENCE = USER_BROKER_PREFIX + "/presence";

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
