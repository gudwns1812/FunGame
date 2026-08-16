package com.fungame.songquiz.domain.session;

public record GameSkipInfo(
        boolean isSkip,
        int skipCount,
        int totalCount
) {
}
