package com.fungame.songquiz.domain.room;

import com.fungame.songquiz.enums.PlayerStatus;

public record MemberLocation(PlayerStatus status, Long roomId) {

    private static final MemberLocation LOBBY = new MemberLocation(PlayerStatus.LOBBY, null);

    public static MemberLocation lobby() {
        return LOBBY;
    }

    public static MemberLocation in(GameRoom room) {
        PlayerStatus status = room.isPlaying() ? PlayerStatus.PLAYING : PlayerStatus.WAITING;

        return new MemberLocation(status, room.getRoomId());
    }

    public boolean isInLobby() {
        return roomId == null;
    }

    public boolean isWaitingIn(Long roomId) {
        return status == PlayerStatus.WAITING && roomId.equals(this.roomId);
    }
}
