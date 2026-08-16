package com.fungame.songquiz.domain.session;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SkipVotes {

    private final Set<Long> voters = ConcurrentHashMap.newKeySet();

    public void add(Long memberId) {
        voters.add(memberId);
    }

    public void remove(Long memberId) {
        voters.remove(memberId);
    }

    public void clear() {
        voters.clear();
    }

    public boolean isThresholdReached(int playerCount) {
        return voters.size() >= requiredVotes(playerCount);
    }

    private static int requiredVotes(int playerCount) {
        return Math.max(1, playerCount - 1);
    }
}
