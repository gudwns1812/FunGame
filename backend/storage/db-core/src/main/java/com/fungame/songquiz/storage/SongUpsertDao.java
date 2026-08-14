package com.fungame.songquiz.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SongUpsertDao {

    private static final String ANSWER_DELIMITER = ",";

    private static final String UPSERT = """
            insert into song_entity
                (title, singer, categories, release_date, video_link, play_seconds, answers, hint)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                title = values(title),
                singer = values(singer),
                categories = values(categories),
                release_date = values(release_date),
                play_seconds = values(play_seconds),
                answers = values(answers),
                hint = values(hint)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void upsertByVideoLink(SongEntity.Quiz quiz, String videoLink) {
        jdbcTemplate.update(UPSERT,
                quiz.title(),
                quiz.singer(),
                toJson(quiz.categories()),
                quiz.releaseDate(),
                videoLink,
                quiz.playSeconds(),
                toAnswerColumn(quiz.answers()),
                quiz.hint());
    }

    private String toJson(List<?> categories) {
        if (categories == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(categories);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("카테고리를 JSON 으로 바꾸지 못했다: " + categories, e);
        }
    }

    private static String toAnswerColumn(List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }

        return String.join(ANSWER_DELIMITER, answers);
    }
}
