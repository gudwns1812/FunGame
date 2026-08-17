package com.fungame.songquiz.client.discord;

import java.util.List;

public record DiscordEmbed(
        String title,
        List<Field> fields
) {

    public record Field(
            String name,
            String value
    ) {
    }
}
