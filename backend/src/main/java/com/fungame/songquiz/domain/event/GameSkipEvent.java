package com.fungame.songquiz.domain.event;

public record GameSkipEvent(
        Long roomId,
        int skipCount,
        int totalCount
) {
}
