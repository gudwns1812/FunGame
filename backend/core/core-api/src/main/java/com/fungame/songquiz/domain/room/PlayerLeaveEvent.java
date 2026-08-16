package com.fungame.songquiz.domain.room;

public record PlayerLeaveEvent(Long roomId, Long memberId, String nickname) {
}
