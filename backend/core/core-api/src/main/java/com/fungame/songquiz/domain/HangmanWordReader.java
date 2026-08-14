package com.fungame.songquiz.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 행맨 게임의 단어를 단어 공급자로부터 획득하여 
 * 게임 객체 생성을 담당하는 Reader(Implementation) 계층입니다.
 */
@Component
@RequiredArgsConstructor
public class HangmanWordReader {
    private final HangmanWordProvider wordProvider;

    /**
     * 공급된 단어를 기반으로 새로운 행맨 게임 인스턴스를 생성합니다.
     * @param difficulty 난이도 (1-5)
     * @return 초기화된 HangmanGame 객체 (플레이어 목록은 추후 설정)
     */
    public HangmanGame create(int difficulty) {
        String word = wordProvider.getWord(difficulty);
        return HangmanGame.create(word);
    }
}
