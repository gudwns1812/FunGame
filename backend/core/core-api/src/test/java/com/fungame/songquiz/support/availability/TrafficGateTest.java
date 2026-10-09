package com.fungame.songquiz.support.availability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class TrafficGateTest {

    @TempDir
    private Path stateDir;

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

    @Test
    @DisplayName("트래픽을 받다가 프로세스가 죽어 다시 뜨면 기동 설정과 상관없이 받는 상태로 돌아온다.")
    void resumesAcceptingAfterRestart() {
        TrafficGate serving = new TrafficGate(false, stateFile(), publisher);
        serving.accept();

        TrafficGate restarted = new TrafficGate(false, stateFile(), publisher);

        assertThat(restarted.isAccepting()).isTrue();
    }

    @Test
    @DisplayName("트래픽을 끊은 채 다시 뜨면 끊긴 상태로 돌아온다. 배포 중 대기시킨 쪽이 멋대로 받지 않는다.")
    void staysRefusingAfterRestart() {
        TrafficGate draining = new TrafficGate(true, stateFile(), publisher);
        draining.refuse();

        TrafficGate restarted = new TrafficGate(true, stateFile(), publisher);

        assertThat(restarted.isAccepting()).isFalse();
    }

    @Test
    @DisplayName("상태 파일에 적힌 값이 기동 설정보다 앞선다. 배포 스크립트는 기동 전에 이 파일로 대기를 지시한다.")
    void stateFileWinsOverStartupSetting() throws IOException {
        Files.writeString(stateDir.resolve("accepting"), "false");

        TrafficGate gate = new TrafficGate(true, stateFile(), publisher);

        assertThat(gate.isAccepting()).isFalse();
    }

    @Test
    @DisplayName("상태 파일이 아직 없으면 기동 설정을 따른다.")
    void followsStartupSettingWithoutStateFile() {
        TrafficGate gate = new TrafficGate(false, stateFile(), publisher);

        assertThat(gate.isAccepting()).isFalse();
    }

    @Test
    @DisplayName("상태 파일을 알아볼 수 없으면 기동 설정을 따른다.")
    void followsStartupSettingWhenStateFileIsUnreadable() throws IOException {
        Files.writeString(stateDir.resolve("accepting"), "maybe");

        TrafficGate gate = new TrafficGate(true, stateFile(), publisher);

        assertThat(gate.isAccepting()).isTrue();
    }

    private String stateFile() {
        return stateDir.resolve("accepting").toString();
    }

    private ReadinessState lastReadiness() {
        return (ReadinessState) published.stream()
                .filter(AvailabilityChangeEvent.class::isInstance)
                .map(event -> ((AvailabilityChangeEvent<?>) event).getState())
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
