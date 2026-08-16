package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.invite.RoomInviteNotification;

public record RoomInviteCreatedEvent(Long targetMemberId, RoomInviteNotification notification) {
}
