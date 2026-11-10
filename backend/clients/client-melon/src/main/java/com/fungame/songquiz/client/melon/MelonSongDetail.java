package com.fungame.songquiz.client.melon;

import java.time.LocalDate;
import java.util.List;

public record MelonSongDetail(LocalDate releaseDate, List<String> genres) {

    public MelonSongDetail {
        genres = List.copyOf(genres);
    }
}
