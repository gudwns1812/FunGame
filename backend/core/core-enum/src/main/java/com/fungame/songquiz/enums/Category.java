package com.fungame.songquiz.enums;

public enum Category {
    TOTAL, KPOP, POP, JPOP,
    BALLAD, DANCE, RAP, RNB, ROCK, OST,
    // 2026-10 카테고리 개편으로 DB(song_category)에서는 더 이상 쓰지 않는다.
    // Redis 에 남은 방 설정과 곡 추가 요청이 옛 값을 들고 있을 수 있어 바로 지우지 않는다. (BACKEND.md 9번)
    SM, YG, JYP, HYBE, STARSHIP,
    GEN1, GEN2, GEN3, GEN4,
    DEFAULT
}
