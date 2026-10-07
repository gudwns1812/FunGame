package com.fungame.songquiz.storage.redis;

public record StoredRoom(Long roomId, String body, String game, long revision, Long updatedAtMillis) {
}
