package com.fungame.songquiz.storage.redis;

public record ClaimedTimer(String taskKey, long dueAtMillis, long leaseUntilMillis) {
}
