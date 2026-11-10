package com.fungame.songquiz.client.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GeminiTitleReader {

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final String PROMPT = """
            아래 JSON 은 노래 제목 목록이다. 노래 맞히기 게임에서 초성 힌트와 정답 판정에 쓴다.
            각 항목마다 다음을 만들어라.
            - pronunciation: 한국 사람이 이 제목을 소리 내어 읽는 대로 적은 한글 표기 가운데 가장 흔한 것 하나.
              영어는 한국에서 흔히 쓰는 외래어 표기로, 숫자와 기호는 노래 제목으로 부를 때 읽는 대로 적는다.
              원래 제목의 단어 경계에서 띄어 쓴다. 한글 음절과 공백만 쓴다.
            - aliases: pronunciation 말고 사람들이 이 제목을 소리 나는 대로 한글로 적을 때 쓸 만한 다른 표기를 8개까지.
              외래어 표기가 갈리는 것(홈스윗홈 · 홈스위트홈, 위플래시 · 위플래쉬), 숫자 · 기호를 다르게 읽는 것 같은 것이다.
              비슷하다고 생각하는 표기는 빠뜨리지 않는다. 없으면 빈 배열.
            - id: 입력의 id 를 그대로.
            singer 는 읽는 법을 정하는 데 참고만 한다.

            """;
    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "ARRAY",
            "items", Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "id", Map.of("type", "STRING"),
                            "pronunciation", Map.of("type", "STRING"),
                            "aliases", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
                    "required", List.of("id", "pronunciation", "aliases")));
    private static final TypeReference<List<GeminiReading>> READINGS = new TypeReference<>() {
    };

    record Part(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(String role, List<Part> parts) {
    }

    record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema, double temperature) {
    }

    record GenerateRequest(List<Content> contents, GenerationConfig generationConfig) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates) {
    }

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    public GeminiTitleReader(RestClient.Builder restClientBuilder,
                             @Value("${client.gemini.api-key:}") String apiKey,
                             @Value("${client.gemini.model}") String model) {
        this(restClientBuilder.requestFactory(timeoutRequestFactory()).build(), apiKey, model);
    }

    GeminiTitleReader(RestClient restClient, String apiKey, String model) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Optional<List<GeminiReading>> read(List<GeminiTitle> titles) {
        if (titles.isEmpty()) {
            return Optional.of(List.of());
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini 키가 없어 제목 {}개를 읽지 않았다", titles.size());
            return Optional.empty();
        }

        try {
            GenerateResponse response = restClient.post()
                    .uri(ENDPOINT, model)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestOf(titles))
                    .retrieve()
                    .body(GenerateResponse.class);

            Optional<String> text = firstText(response);
            if (text.isEmpty()) {
                log.warn("Gemini 응답에 후보가 없다. 제목 {}개", titles.size());
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(text.get(), READINGS));
        } catch (RestClientException | JsonProcessingException e) {
            log.warn("Gemini 로 제목 {}개를 읽지 못했다", titles.size(), e);
            return Optional.empty();
        }
    }

    private GenerateRequest requestOf(List<GeminiTitle> titles) throws JsonProcessingException {
        String prompt = PROMPT + objectMapper.writeValueAsString(titles);

        return new GenerateRequest(
                List.of(new Content("user", List.of(new Part(prompt)))),
                new GenerationConfig(MediaType.APPLICATION_JSON_VALUE, RESPONSE_SCHEMA, 0));
    }

    private static Optional<String> firstText(GenerateResponse response) {
        if (response == null || response.candidates() == null) {
            return Optional.empty();
        }

        return response.candidates().stream()
                .map(Candidate::content)
                .filter(content -> content != null && content.parts() != null)
                .flatMap(content -> content.parts().stream())
                .map(Part::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst();
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return requestFactory;
    }
}
