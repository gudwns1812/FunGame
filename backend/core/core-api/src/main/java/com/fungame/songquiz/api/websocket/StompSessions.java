package com.fungame.songquiz.api.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StompSessions {

    private final Map<String, Long> memberIdBySessionId = new ConcurrentHashMap<>();

    public void add(String sessionId, Long memberId) {
        memberIdBySessionId.put(sessionId, memberId);
    }

    public Long remove(String sessionId) {
        return memberIdBySessionId.remove(sessionId);
    }

    public int countSessionsOf(Long memberId) {
        return (int) memberIdBySessionId.values().stream()
                .filter(memberId::equals)
                .count();
    }

    public int count() {
        return memberIdBySessionId.size();
    }
}
