package com.fungame.songquiz.support.extern;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class YoutubeScraper {

    private static final String SEARCH_URL = "https://www.youtube.com/results?search_query=";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final Pattern VIDEO_ID = Pattern.compile("\"videoId\":\"([\\w-]{11})\"");
    private static final int TIMEOUT_MILLIS = 5000;

    public Optional<String> findVideoId(String title, String singer) {
        String query = (singer + " " + title + " Lyrics").replace(" ", "+");

        try {
            String html = Jsoup.connect(SEARCH_URL + query)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .get()
                    .html();

            Matcher matcher = VIDEO_ID.matcher(html);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }

            log.warn("유튜브 검색 결과에서 영상 id 를 찾지 못했다: {} - {}", singer, title);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("유튜브 검색에 실패했다: {} - {}", singer, title, e);
            return Optional.empty();
        }
    }
}
