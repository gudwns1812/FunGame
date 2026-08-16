package com.fungame.songquiz.domain.quiz;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HangmanGameCreateInfo implements GameCreateInfo {
    private final int difficulty;
}
