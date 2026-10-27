package com.fungame.songquiz.support.logging;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

public final class LogContext implements AutoCloseable {

    public static final String REQUEST_ID = "requestId";
    public static final String WS_SESSION = "wsSession";
    public static final String MEMBER_ID = "memberId";
    public static final String ROOM_ID = "roomId";
    public static final String TIMER_TASK = "timerTask";

    private final Map<String, String> replaced = new HashMap<>();

    private LogContext() {
    }

    public static LogContext open() {
        return new LogContext();
    }

    public LogContext put(String key, Object value) {
        if (value == null) {
            return this;
        }

        if (!replaced.containsKey(key)) {
            replaced.put(key, MDC.get(key));
        }
        MDC.put(key, String.valueOf(value));
        return this;
    }

    @Override
    public void close() {
        replaced.forEach(LogContext::restore);
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            MDC.remove(key);
            return;
        }

        MDC.put(key, previous);
    }
}
