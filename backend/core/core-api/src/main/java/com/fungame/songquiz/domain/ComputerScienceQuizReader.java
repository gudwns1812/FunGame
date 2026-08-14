package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.ComputerScienceEntity;
import com.fungame.songquiz.storage.ComputerScienceRepository;
import com.fungame.songquiz.enums.CSQuizDifficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ComputerScienceQuizReader {

    private final ComputerScienceRepository computerScienceRepository;

    public List<ComputerScienceQuiz> getRandomCSQuizWithCount(int totalRound, CSQuizDifficulty difficulty) {
        List<ComputerScienceEntity> candidates =
                new ArrayList<>(computerScienceRepository.findByDifficultyIn(difficulty.andEasier()));

        Collections.shuffle(candidates);

        return candidates.stream()
                .limit(totalRound)
                .map(this::toDomain)
                .toList();
    }

    private ComputerScienceQuiz toDomain(ComputerScienceEntity entity) {
        return ComputerScienceQuiz.of(
                entity.getField(),
                entity.getContent(),
                entity.getAnswers(),
                entity.getExplanation(),
                entity.getDifficulty()
        );
    }
}
