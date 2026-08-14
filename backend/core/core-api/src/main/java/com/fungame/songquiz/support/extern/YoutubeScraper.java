package com.fungame.songquiz.support.extern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class YoutubeScraper {


    public String getVideoId(String title, String singer) {
        try {
            String query = singer + " " + title + " Lyrics";
            // 유튜브 검색 결과 페이지 주소
            String url = "https://www.youtube.com/results?search_query=" + query.replace(" ", "+");

            // 페이지 HTML 가져오기
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            String html = doc.html();
            // HTML 내에서 "videoId":"xxxx" 형태를 찾아냄
            int index = html.indexOf("\"videoId\":\"");
            if (index != -1) {
                return html.substring(index + 11, index + 22);
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        return "";
    }
}
