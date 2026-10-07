package com.fungame.songquiz.support.availability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fungame.songquiz.support.ApiIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TrafficEndpointTest extends ApiIntegrationTest {

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private TrafficGate trafficGate;

    private final TestRestTemplate management = new TestRestTemplate();

    @Test
    @DisplayName("관리 포트로 트래픽을 끊고 다시 열 수 있다. 배포 스크립트가 이 엔드포인트로 전환한다.")
    void switchesTrafficThroughTheManagementPort() {
        assertThat(acceptingAccordingToEndpoint()).isTrue();

        ResponseEntity<Map> refused = management.postForEntity(
                managementUrl("/actuator/traffic"), Map.of("accepting", false), Map.class);

        assertThat(refused.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(trafficGate.isAccepting()).isFalse();
        assertThat(acceptingAccordingToEndpoint()).isFalse();
    }

    @Test
    @DisplayName("트래픽을 끊으면 readiness 가 내려간다. 로드밸런서가 이걸 보고 이 인스턴스를 뺀다.")
    void readinessFollowsTheGate() {
        assertThat(readinessStatus()).isEqualTo(HttpStatus.OK);

        trafficGate.refuse();

        assertThat(readinessStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("트래픽을 끊어도 liveness 는 살아 있다. 드레인 중인 인스턴스를 재시작하면 안 된다.")
    void livenessStaysUpWhileDraining() {
        trafficGate.refuse();

        ResponseEntity<String> liveness =
                management.getForEntity(managementUrl("/actuator/health/liveness"), String.class);

        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private boolean acceptingAccordingToEndpoint() {
        Map<?, ?> body = management.getForObject(managementUrl("/actuator/traffic"), Map.class);
        return (boolean) body.get("accepting");
    }

    private HttpStatus readinessStatus() {
        return (HttpStatus) management
                .getForEntity(managementUrl("/actuator/health/readiness"), String.class)
                .getStatusCode();
    }

    private String managementUrl(String path) {
        return "http://localhost:" + managementPort + path;
    }
}
