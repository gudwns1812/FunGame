package com.fungame.songquiz.storage.redis;

import java.time.Instant;

public record StoredRoomInvite(
        String inviteId,
        Long roomId,
        Long inviterMemberId,
        Long targetMemberId,
        Instant expiresAt
) {
}
