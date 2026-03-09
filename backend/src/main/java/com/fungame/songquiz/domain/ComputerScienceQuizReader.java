package com.fungame.songquiz.domain;

import com.fungame.songquiz.storage.ComputerScienceEntity;
import com.fungame.songquiz.storage.ComputerScienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ComputerScienceQuizReader {

    private final ComputerScienceRepository computerScienceRepository;

    public List<ComputerScienceQuiz> getRandomCSQuizWithCount(int totalRound) {
        List<ComputerScienceEntity> computerScienceEntities = computerScienceRepository.findAll();

        Collections.shuffle(computerScienceEntities);

        return computerScienceEntities.stream()
                .limit(totalRound)
                .map(ComputerScienceEntity::toDomain)
                .toList();
    }
}
