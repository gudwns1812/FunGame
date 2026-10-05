package com.fungame.songquiz.api.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public final class BroadcastLoss {

    public static final String SERIALIZE = "serialize";
    public static final String QUEUE_FULL = "queue_full";
    public static final String STALE = "stale";
    public static final String ERROR = "error";

    private static final String NAME = "fungame.broadcast.dropped";

    private BroadcastLoss() {
    }

    public static Counter counter(MeterRegistry meterRegistry, String reason) {
        return Counter.builder(NAME)
                .tag("reason", reason)
                .description("다른 인스턴스로 내보내지 못한 브로드캐스트 수")
                .register(meterRegistry);
    }
}
