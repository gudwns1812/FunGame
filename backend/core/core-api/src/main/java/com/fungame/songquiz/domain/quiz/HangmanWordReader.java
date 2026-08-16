package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.storage.HangmanWordEntity;
import com.fungame.songquiz.storage.HangmanWordRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HangmanWordReader {

    private final HangmanWordRepository hangmanWordRepository;

    @Transactional(readOnly = true)
    public HangmanWord findRandomByDifficulty(int difficulty) {
        return hangmanWordRepository.findRandomByDifficulty(difficulty)
                .map(HangmanWordReader::toWord)
                .orElseThrow(() -> new CoreException(ErrorType.HANGMAN_WORD_FETCH_FAILED));
    }

    private static HangmanWord toWord(HangmanWordEntity entity) {
        return new HangmanWord(entity.getWord(), entity.getDifficulty());
    }
}
