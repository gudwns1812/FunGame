package com.fungame.songquiz.storage;

import com.fungame.songquiz.domain.CSQuizDifficulty;
import com.fungame.songquiz.domain.ComputerScienceQuiz;
import com.fungame.songquiz.storage.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ComputerScienceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String field;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Convert(converter = StringListConverter.class)
    private List<String> answers;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    private CSQuizDifficulty difficulty;

    public ComputerScienceQuiz toDomain() {
        return ComputerScienceQuiz.of(field, content, answers, explanation, difficulty);
    }
}
