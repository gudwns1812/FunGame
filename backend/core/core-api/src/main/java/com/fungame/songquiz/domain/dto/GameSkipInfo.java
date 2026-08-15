package com.fungame.songquiz.domain.dto;

public record GameSkipInfo(
        boolean isSkip,
        int skipCount,
        int totalCount
) {
}
