package com.fungame.songquiz.support.error;

public record ErrorMessage(
        ErrorCode code,
        String message
) {
}
