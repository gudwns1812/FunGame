package com.fungame.songquiz.domain;

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
    public HangmanGame create(int difficulty) {
        String word = hangmanWordRepository.findRandomByDifficulty(difficulty)
                .map(HangmanWordEntity::getWord)
                .orElseThrow(() -> new CoreException(ErrorType.HANGMAN_WORD_FETCH_FAILED));

        return HangmanGame.create(word);
    }
}
