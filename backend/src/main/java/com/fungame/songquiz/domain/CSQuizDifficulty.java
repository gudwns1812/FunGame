package com.fungame.songquiz.domain;

import java.util.EnumSet;
import java.util.Set;

public enum CSQuizDifficulty {
    EASY, NORMAL, HARD;

    public Set<CSQuizDifficulty> andEasier() {
        return EnumSet.range(EASY, this);
    }
}
