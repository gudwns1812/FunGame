package com.fungame.songquiz.domain.room;

public record LeaveResult(boolean destroyed, boolean wasPlaying, String nickname, RoomStateInfo state) {
}
