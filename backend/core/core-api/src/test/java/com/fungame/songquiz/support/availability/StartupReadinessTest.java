package com.fungame.songquiz.support.availability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 스프링 부트는 기동이 끝나면 스스로 readiness 를 ACCEPTING_TRAFFIC 으로 올린다. 그래서 게이트를 닫은
 * 채로 떠도 로드밸런서에게는 멀쩡해 보인다 — 대기만 시키려던 인스턴스로 실사용자 요청이 들어간다.
 *
 * <p>여기서는 실제로 애플리케이션을 띄워 그 순서까지 겪어 본다. 게이트 상태만 보는 단위 테스트로는
 * 부트가 나중에 덮어쓰는 것을 잡지 못한다.
 */
@SpringBootTest(classes = StartupReadinessTest.GateOnly.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "app.traffic.accept-on-startup=false")
class StartupReadinessTest {

    @Configuration
    @Import({TrafficGate.class, ApplicationAvailabilityBean.class})
    static class GateOnly {
    }

    @Autowired
    private TrafficGate trafficGate;

    @Autowired
    private ApplicationAvailability applicationAvailability;

    @Test
    @DisplayName("대기 상태로 뜨면 readiness 도 내려가 있다. 로드밸런서가 이걸 보고 트래픽을 넣는다.")
    void readinessIsDownWhenStartedRefusing() {
        assertThat(trafficGate.isAccepting()).isFalse();

        assertThat(applicationAvailability.getState(ReadinessState.class))
                .isEqualTo(ReadinessState.REFUSING_TRAFFIC);
    }

    @Test
    @DisplayName("대기 상태로 떠 있다가 트래픽을 받기 시작하면 readiness 가 올라간다.")
    void readinessComesUpWhenTrafficIsLetIn() {
        trafficGate.accept();

        assertThat(applicationAvailability.getState(ReadinessState.class))
                .isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
    }
}
