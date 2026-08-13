package com.fungame.songquiz.domain.event;

public record PlayerJoinEvent(Long roomId, Long memberId, String nickname) {
}
