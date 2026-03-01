package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.Category;
import com.fungame.songquiz.domain.Song;
import com.fungame.songquiz.storage.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SongEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String singer;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false)
    private String videoLink;

    @Column(nullable = false)
    private int playSeconds;

    @Convert(converter = StringListConverter.class)
    private List<String> answers;

    public Song toDomain() {
        return Song.of(title, singer, category, releaseDate, videoLink, playSeconds, answers);
    }
}
