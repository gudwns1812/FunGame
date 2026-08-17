package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.Category;
import com.fungame.songquiz.storage.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SongEntity {

    private static final int CATEGORY_LENGTH = 32;
    private static final int SONGS_PER_CATEGORY_FETCH = 100;

    public record Quiz(
            String title,
            String singer,
            List<Category> categories,
            LocalDate releaseDate,
            int playSeconds,
            List<String> answers,
            String hint
    ) {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String singer;

    @ElementCollection
    @CollectionTable(name = "song_category", joinColumns = @JoinColumn(name = "song_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = CATEGORY_LENGTH)
    @BatchSize(size = SONGS_PER_CATEGORY_FETCH)
    private List<Category> categories;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false)
    private String videoLink;

    @Column(nullable = false)
    private int playSeconds;

    @Convert(converter = StringListConverter.class)
    private List<String> answers;

    private String hint;

    public static SongEntity open(Quiz quiz, String videoLink) {
        SongEntity entity = new SongEntity();
        entity.videoLink = videoLink;
        entity.changeQuiz(quiz);

        return entity;
    }

    public void changeQuiz(Quiz quiz) {
        this.title = quiz.title();
        this.singer = quiz.singer();
        this.categories = quiz.categories();
        this.releaseDate = quiz.releaseDate();
        this.playSeconds = quiz.playSeconds();
        this.answers = quiz.answers();
        this.hint = quiz.hint();
    }
}
