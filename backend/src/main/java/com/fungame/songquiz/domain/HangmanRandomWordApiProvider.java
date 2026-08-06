package com.fungame.songquiz.domain;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
                List<String> words = readWords(resource);
                wordsByDifficulty.put(i, words);
                log.info("난이도 {} 단어 {}개 로드 완료", i, words.size());
            } catch (IOException e) {
                log.error("난이도 {} 단어 파일 로드 실패", i, e);
                throw new RuntimeException("단어 파일 로드 실패: difficulty_" + i + ".txt");
            }
        }
    }

    private List<String> readWords(ClassPathResource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(word -> !word.isEmpty())
                    .toList();
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
