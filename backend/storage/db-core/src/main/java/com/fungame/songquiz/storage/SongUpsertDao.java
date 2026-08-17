package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.Category;
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
                (title, singer, release_date, video_link, play_seconds, answers, hint)
            values (?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                title = values(title),
                singer = values(singer),
                release_date = values(release_date),
                play_seconds = values(play_seconds),
                answers = values(answers),
                hint = values(hint)
            """;

    private static final String FIND_ID_BY_VIDEO_LINK = "select id from song_entity where video_link = ?";

    private static final String DELETE_CATEGORIES = "delete from song_category where song_id = ?";

    private static final String INSERT_CATEGORY = "insert into song_category (song_id, category) values (?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public void upsertByVideoLink(SongEntity.Quiz quiz, String videoLink) {
        jdbcTemplate.update(UPSERT,
                quiz.title(),
                quiz.singer(),
                quiz.releaseDate(),
                videoLink,
                quiz.playSeconds(),
                toAnswerColumn(quiz.answers()),
                quiz.hint());

        replaceCategories(jdbcTemplate.queryForObject(FIND_ID_BY_VIDEO_LINK, Long.class, videoLink),
                quiz.categories());
    }

    private void replaceCategories(Long songId, List<Category> categories) {
        jdbcTemplate.update(DELETE_CATEGORIES, songId);

        if (categories == null || categories.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(INSERT_CATEGORY, categories.stream()
                .distinct()
                .map(category -> new Object[]{songId, category.name()})
                .toList());
    }

    private static String toAnswerColumn(List<String> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }

        return String.join(ANSWER_DELIMITER, answers);
    }
}
