package com.fungame.songquiz.support.availability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class TrafficGateTest {

    private final List<Object> published = new ArrayList<>();
    private final ApplicationEventPublisher publisher = new ApplicationEventPublisher() {
        @Override
        public void publishEvent(Object event) {
            published.add(event);
        }

        @Override
        public void publishEvent(ApplicationEvent event) {
            published.add(event);
        }
    };

    @Test
    @DisplayName("트래픽을 받는 상태로 뜨면 전역 작업을 돌린다.")
    void opensWhenStartedAccepting() {
        TrafficGate gate = new TrafficGate(true, publisher);

        assertThat(gate.isAccepting()).isTrue();
    }

    @Test
    @DisplayName("트래픽을 받지 않는 상태로 뜨면 전역 작업을 멈춘다. 블루-그린에서 기동만 해 둔 쪽이다.")
    void staysClosedWhenStartedRefusing() {
        TrafficGate gate = new TrafficGate(false, publisher);

        assertThat(gate.isAccepting()).isFalse();
    }

    @Test
    @DisplayName("트래픽을 받기 시작하면 열리고, 끊으면 닫힌다.")
    void opensAndClosesOnDemand() {
        TrafficGate gate = new TrafficGate(false, publisher);

        gate.accept();
        assertThat(gate.isAccepting()).isTrue();

        gate.refuse();
        assertThat(gate.isAccepting()).isFalse();
    }

    @Test
    @DisplayName("상태가 바뀌면 readiness 로 알린다. 로드밸런서가 이 상태를 보고 넣고 뺀다.")
    void tellsReadinessWhenItChanges() {
        TrafficGate gate = new TrafficGate(false, publisher);

        gate.accept();
        assertThat(lastReadiness()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);

        gate.refuse();
        assertThat(lastReadiness()).isEqualTo(ReadinessState.REFUSING_TRAFFIC);
    }

    @Test
    @DisplayName("이미 그 상태면 다시 알리지 않는다.")
    void doesNotRepeatItself() {
        TrafficGate gate = new TrafficGate(true, publisher);

        gate.accept();

        assertThat(published).isEmpty();
    }

    private ReadinessState lastReadiness() {
        return (ReadinessState) published.stream()
                .filter(AvailabilityChangeEvent.class::isInstance)
                .map(event -> ((AvailabilityChangeEvent<?>) event).getState())
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
