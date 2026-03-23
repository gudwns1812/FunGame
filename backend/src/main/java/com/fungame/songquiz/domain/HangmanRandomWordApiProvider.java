package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * random-word-api를 사용하여 행맨 게임 단어를 무작위로 가져오는 공급자입니다.
 */
@Slf4j
@Component
public class HangmanRandomWordApiProvider implements HangmanWordProvider {

    private final Map<Integer, List<String>> wordsByDifficulty = new HashMap<>();
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        for (int i = 1; i < 5; i++) {
            try {
                ClassPathResource resource = new ClassPathResource("words/difficulty_" + i + ".txt");
                List<String> words = Files.readAllLines(resource.getFile().toPath());
                wordsByDifficulty.put(i, words);
                log.info("난이도 {} 단어 {}개 로드 완료", i, words.size());
            } catch (IOException e) {
                log.error("난이도 {} 단어 파일 로드 실패", i, e);
                throw new RuntimeException("단어 파일 로드 실패: difficulty_" + i + ".txt");
            }
        }
    }

    @Override
    public String getWord(int difficulty) {
        List<String> words = wordsByDifficulty.get(difficulty);

        if (words == null || words.isEmpty()) {
            log.error("난이도 {}에 해당하는 단어 목록 없음", difficulty);
            throw new CoreException(ErrorType.HANGMAN_WORD_FETCH_FAILED);
        }

        String word = words.get(random.nextInt(words.size()));
        log.info("난이도 {} 단어 선택: {}", difficulty, word);
        return word;
    }
}