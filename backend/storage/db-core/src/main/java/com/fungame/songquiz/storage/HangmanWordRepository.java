package com.fungame.songquiz.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HangmanWordRepository extends JpaRepository<HangmanWordEntity, Long> {

    @Query(value = "SELECT * FROM hangman_word WHERE difficulty = :difficulty ORDER BY RAND() LIMIT 1",
            nativeQuery = true)
    Optional<HangmanWordEntity> findRandomByDifficulty(@Param("difficulty") int difficulty);
}
