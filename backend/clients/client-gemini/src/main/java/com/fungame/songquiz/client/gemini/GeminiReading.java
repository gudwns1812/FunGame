package com.fungame.songquiz.client.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiReading(String id, String pronunciation, List<String> aliases) {

    public GeminiReading {
        aliases = aliases == null ? List.of() : aliases.stream().filter(Objects::nonNull).toList();
    }
}
