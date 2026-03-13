package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.SongEntity;
import com.fungame.songquiz.storage.SongRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;

    public List<Long> getRandomSongIds(int count) {
        List<Long> allIds = songRepository.findAll().stream()
                .map(SongEntity::getId)
                .collect(Collectors.toList());

        Collections.shuffle(allIds);

        return allIds.stream()
                .limit(count)
                .collect(Collectors.toList());
    }


    public void createSongQuiz(Song song) {
        String videoLink = getVideoId(song.getTitle(), song.getSinger());

        boolean exists = songRepository.existsBySingerAndTitle(song.getSinger(), song.getTitle());
        if (exists) {
            throw new CoreException(ErrorType.QUIZ_DUPLICATE_ERROR);
        }

        SongEntity newSong = SongEntity.builder()
                .title(song.getTitle())
                .singer(song.getSinger())
                .categories(song.getCategories())
                .playSeconds(song.getPlaySeconds())
                .answers(new ArrayList<>(song.getAnswers()))
                .videoLink(videoLink)
                .releaseDate(song.getReleaseDate())
                .hint(song.getHint())
                .build();

        songRepository.save(newSong);
    }

    private String getVideoId(String title, String singer) {
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
