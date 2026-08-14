package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ComputerScienceStore {

    private final ComputerScienceRepository computerScienceRepository;

    public List<ComputerScienceEntity> findByDifficultyIn(Collection<CSQuizDifficulty> difficulties) {
        return computerScienceRepository.findByDifficultyIn(difficulties);
    }
}
