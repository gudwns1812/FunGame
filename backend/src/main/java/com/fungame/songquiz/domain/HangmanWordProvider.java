package com.fungame.songquiz.domain;

/**
 * 행맨 게임에서 사용할 단어를 공급하는 인터페이스입니다.
 */
public interface HangmanWordProvider {
    /**
     * 난이도를 기반으로 퀴즈용 단어를 반환합니다.
     * @param difficulty 난이도 (1-5)
     * @return 퀴즈 단어
     */
    String getWord(int difficulty);
}
