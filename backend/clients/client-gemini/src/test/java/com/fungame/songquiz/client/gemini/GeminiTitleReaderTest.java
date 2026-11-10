package com.fungame.songquiz.client.gemini;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiTitleReaderTest {

    private static final String API_KEY = "test-key";
    private static final String MODEL = "gemini-test";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent";
    private static final List<GeminiTitle> TITLES = List.of(
            new GeminiTitle("38123332", "Whiplash", "aespa"),
            new GeminiTitle("39504779", "0+0", "한로로"));

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    @DisplayName("제목을 한 요청에 모아 보내고 응답 스키마를 고정한다.")
    void sendsAllTitlesInOneRequest() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.type").value("ARRAY"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.items.required.length()").value(3))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(allOf(
                        containsString("38123332"), containsString("Whiplash"), containsString("0+0"))))
                .andRespond(withSuccess(geminiResponse("[]"), MediaType.APPLICATION_JSON));

        reader(API_KEY).read(TITLES);

        server.verify();
    }

    @Test
    @DisplayName("응답 본문 속 JSON 배열을 읽는 법으로 바꾼다.")
    void parsesReadings() {
        String readings = """
                [{"id":"38123332","pronunciation":"위플래시","aliases":["위플래쉬","whiplash"]},
                 {"id":"39504779","pronunciation":"영 더하기 영","aliases":[]}]
                """;
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse(readings), MediaType.APPLICATION_JSON));

        Optional<List<GeminiReading>> result = reader(API_KEY).read(TITLES);

        assertThat(result).contains(List.of(
                new GeminiReading("38123332", "위플래시", List.of("위플래쉬", "whiplash")),
                new GeminiReading("39504779", "영 더하기 영", List.of())));
    }

    @Test
    @DisplayName("한도 초과면 빈 결과다.")
    void emptyWhenRateLimited() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(reader(API_KEY).read(TITLES)).isEmpty();
    }

    @Test
    @DisplayName("서버 오류면 빈 결과다.")
    void emptyWhenServerFails() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(reader(API_KEY).read(TITLES)).isEmpty();
    }

    @Test
    @DisplayName("응답 본문이 JSON 배열이 아니면 빈 결과다.")
    void emptyWhenBodyIsNotJson() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(geminiResponse("위플래시"), MediaType.APPLICATION_JSON));

        assertThat(reader(API_KEY).read(TITLES)).isEmpty();
    }

    @Test
    @DisplayName("후보가 없으면 빈 결과다.")
    void emptyWhenNoCandidates() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        assertThat(reader(API_KEY).read(TITLES)).isEmpty();
    }

    @Test
    @DisplayName("키가 없으면 요청하지 않고 빈 결과다.")
    void skipsWithoutApiKey() {
        assertThat(reader("").read(TITLES)).isEmpty();

        server.verify();
    }

    @Test
    @DisplayName("읽을 제목이 없으면 요청하지 않고 빈 목록이다.")
    void skipsWithoutTitles() {
        assertThat(reader(API_KEY).read(List.of())).contains(List.of());

        server.verify();
    }

    private GeminiTitleReader reader(String apiKey) {
        return new GeminiTitleReader(builder.build(), apiKey, MODEL);
    }

    private static String geminiResponse(String text) {
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + escaped + "\"}],\"role\":\"model\"}}]}";
    }
}
