package com.fungame.songquiz.client.discord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DiscordWebhookSenderTest {

    private static final String WEBHOOK_URL = "https://discord.test/api/webhooks/report";

    private MockRestServiceServer server;
    private DiscordWebhookSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new DiscordWebhookSender(builder, WEBHOOK_URL);
    }

    private static DiscordEmbed embed(String value) {
        return new DiscordEmbed("힌트가 이상하다", List.of(new DiscordEmbed.Field("정답", value)));
    }

    @Test
    @DisplayName("웹훅 주소로 embed 하나를 담아 보낸다.")
    void sendsEmbedToWebhook() {
        server.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.embeds[0].title").value("힌트가 이상하다"))
                .andExpect(jsonPath("$.embeds[0].fields[0].name").value("정답"))
                .andExpect(jsonPath("$.embeds[0].fields[0].value").value("아이유 - 밤편지"))
                .andRespond(withNoContent());

        sender.send(embed("아이유 - 밤편지"));

        server.verify();
    }

    @Test
    @DisplayName("전송이 실패해도 예외를 밖으로 던지지 않는다.")
    void swallowsSendFailure() {
        server.expect(requestTo(WEBHOOK_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatCode(() -> sender.send(embed("아이유 - 밤편지"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("디스코드가 받아주는 길이를 넘는 값은 잘라서 보낸다.")
    void trimsValueTooLongForDiscord() {
        String tooLong = "가".repeat(DiscordWebhookSender.MAX_FIELD_VALUE_LENGTH + 100);

        server.expect(requestTo(WEBHOOK_URL))
                .andExpect(jsonPath("$.embeds[0].fields[0].value")
                        .value(org.hamcrest.Matchers.hasLength(DiscordWebhookSender.MAX_FIELD_VALUE_LENGTH)))
                .andRespond(withNoContent());

        sender.send(embed(tooLong));

        server.verify();
    }
}
