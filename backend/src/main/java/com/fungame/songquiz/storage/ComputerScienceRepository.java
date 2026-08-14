package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.CSQuizDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ComputerScienceRepository extends JpaRepository<ComputerScienceEntity, Long> {

    List<ComputerScienceEntity> findByDifficultyIn(Collection<CSQuizDifficulty> difficulties);
}
