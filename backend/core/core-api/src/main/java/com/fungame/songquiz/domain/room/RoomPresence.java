package com.fungame.songquiz.domain.room;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomPresence {

    private final Map<String, RoomMember> membersBySessionId = new ConcurrentHashMap<>();

    public void arrive(String sessionId, RoomMember member) {
        membersBySessionId.put(sessionId, member);
    }

    public RoomMember depart(String sessionId) {
        return membersBySessionId.remove(sessionId);
    }

    public boolean isConnected(RoomMember member) {
        return membersBySessionId.containsValue(member);
    }

    public int countConnectedIn(Long roomId) {
        return (int) membersBySessionId.values().stream()
                .filter(member -> member.roomId().equals(roomId))
                .map(RoomMember::memberId)
                .distinct()
                .count();
    }
}
