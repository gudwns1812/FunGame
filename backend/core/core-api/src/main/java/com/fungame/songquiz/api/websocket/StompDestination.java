package com.fungame.songquiz.api.websocket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StompDestination {

    public static final String BROKER_PREFIX = "/topic";

    public static final String USER_BROKER_PREFIX = "/queue";

    public static final String APPLICATION_PREFIX = "/app";

    public static final String LOBBY = BROKER_PREFIX + "/lobby";

    public static final String PRESENCE = BROKER_PREFIX + "/presence";

    public static final String INVITE = USER_BROKER_PREFIX + "/invite";

    private static final String ROOM_PREFIX = BROKER_PREFIX + "/room/";

    private static final Pattern ROOM_SCOPED = Pattern.compile(
            "^(" + APPLICATION_PREFIX + "|" + BROKER_PREFIX + ")/room/(?<roomId>\\d{1,18})(/.*)?$");

    private StompDestination() {
    }

    public static Long roomIdIn(String destination) {
        if (destination == null) {
            return null;
        }

        Matcher matcher = ROOM_SCOPED.matcher(destination);
        return matcher.matches() ? Long.valueOf(matcher.group("roomId")) : null;
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
