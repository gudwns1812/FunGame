package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fungame.songquiz.storage.converter.StringListConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "song_scrape_request")
public class SongScrapeRequestEntity {

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
    private int playSeconds;

    @Convert(converter = StringListConverter.class)
    private List<String> answers;

    private String hint;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private SongScrapeRequestEntity(String title, String singer, List<Category> categories, LocalDate releaseDate,
                                    int playSeconds, List<String> answers, String hint) {
        this.title = title;
        this.singer = singer;
        this.categories = categories;
        this.releaseDate = releaseDate;
        this.playSeconds = playSeconds;
        this.answers = answers;
        this.hint = hint;
    }
}
