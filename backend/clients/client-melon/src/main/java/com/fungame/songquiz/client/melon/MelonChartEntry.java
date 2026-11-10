package com.fungame.songquiz.client.melon;

import java.util.List;

public record MelonChartEntry(long songId, String title, List<String> singers, long likes) {

    public MelonChartEntry {
        singers = List.copyOf(singers);
    }
}
