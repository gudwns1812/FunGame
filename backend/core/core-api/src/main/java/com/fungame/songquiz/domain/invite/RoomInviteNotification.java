package com.fungame.songquiz.domain.invite;

import com.fungame.songquiz.enums.GameType;

public record RoomInviteNotification(
        String inviteId,
        Long roomId,
        String roomTitle,
        GameType gameType,
        String inviterNickname,
        long expiresInSeconds
) {
}
