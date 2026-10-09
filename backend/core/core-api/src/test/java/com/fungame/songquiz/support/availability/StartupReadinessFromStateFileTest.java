package com.fungame.songquiz.support.availability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = StartupReadinessFromStateFileTest.GateOnly.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "app.traffic.accept-on-startup=true")
class StartupReadinessFromStateFileTest {

    @Configuration
    @Import({TrafficGate.class, ApplicationAvailabilityBean.class})
    static class GateOnly {
    }

    @DynamicPropertySource
    static void refusingStateFile(DynamicPropertyRegistry registry) {
        registry.add("app.traffic.state-file", StartupReadinessFromStateFileTest::writeRefusingStateFile);
    }

    private static String writeRefusingStateFile() {
        try {
            Path stateFile = Files.createTempDirectory("traffic").resolve("accepting");
            Files.writeString(stateFile, "false");
            return stateFile.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired
    private TrafficGate trafficGate;

    @Autowired
    private ApplicationAvailability applicationAvailability;

    @Test
    @DisplayName("상태 파일이 대기를 지시하면 기동 설정이 받음이어도 readiness 가 내려가 있다.")
    void readinessFollowsStateFileOnStartup() {
        assertThat(trafficGate.isAccepting()).isFalse();

        assertThat(applicationAvailability.getState(ReadinessState.class))
                .isEqualTo(ReadinessState.REFUSING_TRAFFIC);
    }
}
