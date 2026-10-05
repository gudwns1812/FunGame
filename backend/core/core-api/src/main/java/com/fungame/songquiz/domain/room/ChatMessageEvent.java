package com.fungame.songquiz.domain.room;

public record ChatMessageEvent(Long roomId, Long memberId, String message) {
}
