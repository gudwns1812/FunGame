package com.fungame.songquiz.domain.invite;

public record SentInvite(long expiresInSeconds) {

    public static SentInvite from(RoomInviteNotification notification) {
        return new SentInvite(notification.expiresInSeconds());
    }
}
