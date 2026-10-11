package com.fungame.songquiz.support.monitoring;

import com.fungame.songquiz.support.ApiIntegrationTest;
import com.fungame.songquiz.support.availability.TrafficGate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementEndpointTest extends ApiIntegrationTest {

    @LocalManagementPort
    private int managementPort;

    @LocalServerPort
    private int servicePort;

    @Autowired
    private TrafficGate trafficGate;

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
    @DisplayName("메트릭에 인스턴스 라벨이 붙는다.")
    void metricsCarryInstanceLabel() {
        ResponseEntity<String> response = get(managementPort, "/actuator/prometheus");

        assertThat(response.getBody()).contains("instance=\"itest\"");
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

    @Test
    @DisplayName("직접 만든 지표가 로그인·접속 한 번 없이도 스크랩에 잡힌다.")
    void prometheusExposesCustomCounters() {
        String scrapeBody = get(managementPort, "/actuator/prometheus").getBody();

        assertThat(samplesOf(scrapeBody, "fungame_logins_total"))
                .as("시도 전에 시계열이 없으면 실패율 알람이 데이터 없음으로 뜬다")
                .hasSize(2);
        assertThat(samplesOf(scrapeBody, "fungame_member_first_seen_total")).hasSize(1);
    }

    @Test
    @DisplayName("트래픽을 받는지가 지표로 나온다. 받으면 1, 끊으면 0 이다.")
    void trafficGateIsExposedAsMetric() {
        assertThat(samplesOf(scrape(), "fungame_traffic_accepting")).singleElement()
                .satisfies(sample -> assertThat(sample).endsWith(" 1.0"));

        trafficGate.refuse();

        assertThat(samplesOf(scrape(), "fungame_traffic_accepting")).singleElement()
                .satisfies(sample -> assertThat(sample).endsWith(" 0.0"));
    }

    @Test
    @DisplayName("HTTP 요청 지표가 응답시간 SLO 버킷으로 나온다. 버킷은 정한 다섯 개와 +Inf 뿐이다.")
    void httpRequestsArePublishedWithSloBuckets() {
        get(servicePort, "/game/rooms");

        List<String> bucketBounds = samplesOf(scrape(), "http_server_requests_seconds_bucket").stream()
                .filter(sample -> sample.contains("uri=\"/game/rooms\""))
                .map(ManagementEndpointTest::bucketBoundOf)
                .distinct()
                .toList();

        assertThat(bucketBounds).containsExactlyInAnyOrder("0.05", "0.1", "0.3", "1.0", "3.0", "+Inf");
    }

    private String scrape() {
        return get(managementPort, "/actuator/prometheus").getBody();
    }

    private static String bucketBoundOf(String sample) {
        Matcher matcher = Pattern.compile("le=\"([^\"]+)\"").matcher(sample);
        return matcher.find() ? matcher.group(1) : "";
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
