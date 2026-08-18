package com.fungame.songquiz.domain.member;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LoginMetrics {

    static final String LOGIN_METER = "fungame.logins";
    static final String RESULT_TAG = "result";
    static final String SUCCESS = "success";
    static final String FAIL = "fail";

    private final Counter success;
    private final Counter fail;

    public LoginMetrics(MeterRegistry meterRegistry) {
        // 첫 로그인 전까지 시계열이 없으면 실패율 알람이 데이터 없음으로 뜬다. 미리 등록해 0 을 내보낸다.
        this.success = counter(meterRegistry, SUCCESS);
        this.fail = counter(meterRegistry, FAIL);
    }

    public void loginSucceeded() {
        success.increment();
    }

    public void loginFailed() {
        fail.increment();
    }

    private static Counter counter(MeterRegistry meterRegistry, String result) {
        return Counter.builder(LOGIN_METER)
                .description("로그인 시도 수")
                .tag(RESULT_TAG, result)
                .register(meterRegistry);
    }
}
