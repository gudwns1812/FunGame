package com.fungame.songquiz.domain.invite;


public record RoomInviteCreatedEvent(Long targetMemberId, RoomInviteNotification notification) {
}
