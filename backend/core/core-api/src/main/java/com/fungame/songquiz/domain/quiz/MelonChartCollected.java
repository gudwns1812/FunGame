package com.fungame.songquiz.domain.quiz;

public record MelonChartCollected(Result result, int charted, int popular, int enqueued, int deferred) {

    public enum Result {
        OK, EMPTY, FAILED, GEMINI_FAILED
    }

    static MelonChartCollected empty() {
        return new MelonChartCollected(Result.EMPTY, 0, 0, 0, 0);
    }

    static MelonChartCollected failed() {
        return new MelonChartCollected(Result.FAILED, 0, 0, 0, 0);
    }
}
