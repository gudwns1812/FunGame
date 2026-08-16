package com.fungame.songquiz.domain.session;

public record GameSkipEvent(
        Long roomId,
        int skipCount,
        int totalCount
) {
}
