package com.fungame.songquiz.storage;

public record GameRecordPlayerRow(
        Long memberId,
        boolean won,
        Integer rank,
        Integer score,
        boolean left
) {
}
