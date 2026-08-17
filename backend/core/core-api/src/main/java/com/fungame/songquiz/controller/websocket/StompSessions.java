package com.fungame.songquiz.controller.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 지금 살아 있는 STOMP 세션이 누구의 것인지만 안다.
 * 한 회원이 탭을 여러 개 열면 세션도 그만큼 생긴다.
 */
@Component
public class StompSessions {

    private final Map<String, Long> memberIdBySessionId = new ConcurrentHashMap<>();

    public void add(String sessionId, Long memberId) {
        memberIdBySessionId.put(sessionId, memberId);
    }

    public Long remove(String sessionId) {
        return memberIdBySessionId.remove(sessionId);
    }

    public boolean isConnected(Long memberId) {
        return memberIdBySessionId.containsValue(memberId);
    }

    public int countSessionsOf(Long memberId) {
        return (int) memberIdBySessionId.values().stream()
                .filter(memberId::equals)
                .count();
    }

    public Set<Long> connectedMemberIds() {
        return Set.copyOf(memberIdBySessionId.values());
    }
}
