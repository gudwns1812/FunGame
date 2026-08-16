package com.fungame.songquiz.domain.room;

public record PlayerJoinEvent(Long roomId, Long memberId, String nickname) {
}
