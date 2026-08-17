package com.fungame.songquiz.enums;

public enum ReportReason {
    CONTENT_NOT_SHOWN, CONTENT_WRONG, HINT_WRONG, ANSWER_WRONG, ETC;

    public boolean needsDetail() {
        return this == ETC;
    }
}
