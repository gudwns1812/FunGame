package com.fungame.songquiz.domain.session;

import java.util.Optional;

public record GameTimerTask(Kind kind, Long targetId, int round) {

    private static final String ROOM_SCOPE = "room:";
    private static final String MEMBER_SCOPE = "member:";
    private static final String SEPARATOR = ":";
    private static final int NO_ROUND = 0;
    private static final int KEY_PARTS = 4;

    public enum Kind {
        START_ROUND, OPEN_HINT, END_ROUND, SHOW_RESULT, LEAVE_ROOM
    }

    public static GameTimerTask startRound(Long roomId, int round) {
        return new GameTimerTask(Kind.START_ROUND, roomId, round);
    }

    public static GameTimerTask openHint(Long roomId, int round) {
        return new GameTimerTask(Kind.OPEN_HINT, roomId, round);
    }

    public static GameTimerTask endRound(Long roomId, int round) {
        return new GameTimerTask(Kind.END_ROUND, roomId, round);
    }

    public static GameTimerTask showResult(Long roomId) {
        return new GameTimerTask(Kind.SHOW_RESULT, roomId, NO_ROUND);
    }

    public static GameTimerTask leaveRoom(Long memberId) {
        return new GameTimerTask(Kind.LEAVE_ROOM, memberId, NO_ROUND);
    }

    public static String roomKeyPrefix(Long roomId) {
        return ROOM_SCOPE + roomId + SEPARATOR;
    }

    public String key() {
        String scope = kind == Kind.LEAVE_ROOM ? MEMBER_SCOPE : ROOM_SCOPE;
        return scope + targetId + SEPARATOR + kind + SEPARATOR + round;
    }

    public static Optional<GameTimerTask> fromKey(String key) {
        String[] parts = key.split(SEPARATOR);
        if (parts.length != KEY_PARTS) {
            return Optional.empty();
        }

        try {
            return Optional.of(new GameTimerTask(
                    Kind.valueOf(parts[2]), Long.valueOf(parts[1]), Integer.parseInt(parts[3])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
