package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.Song;
import com.fungame.songquiz.storage.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SongEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String singer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
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

    public Song toDomain() {
        return Song.of(title, singer, categories, releaseDate, videoLink, playSeconds, answers, hint);
    }
}
