package com.fungame.songquiz.support.monitoring;

import com.fungame.songquiz.storage.MySqlTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@Import(MySqlTestContainer.class)
@TestPropertySource(properties = {
        "spring.session.jdbc.initialize-schema=always",
        "app.song-scrape.enabled=false",
        "management.server.port=0"
})
class ManagementEndpointTest {

    @LocalManagementPort
    private int managementPort;

    @LocalServerPort
    private int servicePort;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    private ResponseEntity<String> get(int port, String path) {
        return restTemplate.getForEntity("http://localhost:" + port + path, String.class);
    }

    @Test
    @DisplayName("헬스 엔드포인트는 인증 없이 열려 있다.")
    void healthIsOpen() {
        ResponseEntity<String> response = get(managementPort, "/actuator/health");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("헬스 응답에 상세 정보를 담지 않는다.")
    void healthHidesDetails() {
        assertThat(get(managementPort, "/actuator/health").getBody()).doesNotContain("components");
    }

    @Test
    @DisplayName("프로메테우스 스크랩 엔드포인트가 열려 있다.")
    void prometheusIsOpen() {
        ResponseEntity<String> response = get(managementPort, "/actuator/prometheus");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    @DisplayName("스크랩 결과의 지표마다 application, namespace 라벨이 붙는다.")
    void prometheusTagsEveryMetric() {
        String scrapeBody = get(managementPort, "/actuator/prometheus").getBody();

        assertThat(samplesOf(scrapeBody, "hikaricp_connections"))
                .as("Spring Boot JDBC & HikariCP 대시보드(20729)가 이 두 라벨로 지표를 걸러낸다")
                .isNotEmpty()
                .allSatisfy(sample -> assertThat(sample)
                        .contains("application=\"fungame-backend\"")
                        .contains("namespace=\""));
    }

    private List<String> samplesOf(String scrapeBody, String metricName) {
        return scrapeBody.lines()
                .filter(line -> line.startsWith(metricName + "{"))
                .toList();
    }

    @Test
    @DisplayName("노출하지 않은 엔드포인트는 닫혀 있다.")
    void unexposedEndpointIsClosed() {
        assertThat(get(managementPort, "/actuator/env").getStatusCode())
                .as("노출 목록에 없으면 등록되지 않아 actuator 체인이 잡지 않고 인증에서 막힌다")
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("서비스 포트에는 actuator 가 없다.")
    void servicePortHasNoActuator() {
        assertThat(get(servicePort, "/actuator/health").getStatusCode())
                .as("관리 포트를 분리했으므로 서비스 포트로는 열리지 않아야 한다")
                .isNotEqualTo(HttpStatus.OK);
    }
}
