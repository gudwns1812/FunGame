package com.fungame.songquiz.client.discord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@Profile("prod")
public class DiscordWebhookSender {

    static final int MAX_TITLE_LENGTH = 256;
    static final int MAX_FIELD_VALUE_LENGTH = 1024;

    private record WebhookRequest(List<Embed> embeds) {
    }

    private record Embed(String title, List<Field> fields) {
    }

    private record Field(String name, String value, boolean inline) {
    }

    private final RestClient restClient;
    private final String webhookUrl;

    public DiscordWebhookSender(RestClient.Builder restClientBuilder,
                                @Value("${client.discord.report-webhook-url}") String webhookUrl) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
    }

    public void send(DiscordEmbed embed) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestOf(embed))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("디스코드 웹훅 전송에 실패했습니다. 제목: {}", embed.title(), e);
        }
    }

    private static WebhookRequest requestOf(DiscordEmbed embed) {
        List<Field> fields = embed.fields().stream()
                .map(field -> new Field(field.name(), trimmed(field.value(), MAX_FIELD_VALUE_LENGTH), true))
                .toList();

        return new WebhookRequest(List.of(new Embed(trimmed(embed.title(), MAX_TITLE_LENGTH), fields)));
    }

    private static String trimmed(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
