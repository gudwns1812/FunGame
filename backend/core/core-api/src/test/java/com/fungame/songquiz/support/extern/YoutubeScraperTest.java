package com.fungame.songquiz.support.extern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("external")
class YoutubeScraperTest {

    YoutubeScraper youtubeScraper = new YoutubeScraper();

    @Test
    void 유튜브_검색결과에서_videoId를_잘_가져오는지_확인한다() {
        // Given
        String title = "밤양갱";
        String singer = "비비";

        // When
        Optional<String> videoId = youtubeScraper.findVideoId(title, singer);

        // Then
        assertThat(videoId).isPresent();
        assertThat(videoId.get()).hasSize(11); // 유튜브 ID는 무조건 11자리입니다.
    }
}
