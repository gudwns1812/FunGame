package com.fungame.songquiz.domain.invite;

import com.fungame.songquiz.domain.room.GamePlayer;

import java.time.LocalDateTime;

public record RoomInvite(
        String inviteId,
        Long roomId,
        GamePlayer inviter,
        Long targetMemberId,
        LocalDateTime expiresAt
) {

    public boolean isExpiredAt(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isNotFor(Long memberId) {
        return !targetMemberId.equals(memberId);
    }
}
