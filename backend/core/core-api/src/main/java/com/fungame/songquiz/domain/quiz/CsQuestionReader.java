package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import com.fungame.songquiz.storage.ComputerScienceEntity;
import com.fungame.songquiz.storage.ComputerScienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CsQuestionReader {

    private final ComputerScienceRepository computerScienceRepository;

    @Transactional(readOnly = true)
    public List<CsQuestion> getRandomCSQuizWithCount(int totalRound, CSQuizDifficulty difficulty) {
        List<ComputerScienceEntity> candidates =
                new ArrayList<>(computerScienceRepository.findByDifficultyIn(difficulty.andEasier()));

        Collections.shuffle(candidates);

        return candidates.stream()
                .limit(totalRound)
                .map(this::toDomain)
                .toList();
    }

    private CsQuestion toDomain(ComputerScienceEntity entity) {
        return CsQuestion.of(
                entity.getField(),
                entity.getContent(),
                entity.getAnswers(),
                entity.getExplanation(),
                entity.getDifficulty()
        );
    }
}
