package com.fungame.songquiz.support.extern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeScraperTest {

    YoutubeScraper youtubeScraper = new YoutubeScraper();

    @Test
    void 유튜브_검색결과에서_videoId를_잘_가져오는지_확인한다() {
        // Given
        String title = "밤양갱";
        String singer = "비비";

        // When
        // 주의: 테스트를 위해 잠시 getVideoId 메서드를 public이나 default(접근제어자 생략)로 바꿔주세요.
        String videoId = youtubeScraper.getVideoId(title, singer);

        // Then
        System.out.println("추출된 Video ID: [" + videoId + "]");
        System.out.println("실제 영상 링크: https://www.youtube.com/watch?v=" + videoId);

        assertThat(videoId).isNotBlank();
        assertThat(videoId.length()).isEqualTo(11); // 유튜브 ID는 무조건 11자리입니다.
    }
}