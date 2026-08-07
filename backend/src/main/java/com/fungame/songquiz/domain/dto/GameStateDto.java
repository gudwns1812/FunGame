package com.fungame.songquiz.domain.dto;

import java.util.List;

/**
 * 진행 중인 게임의 현재 스냅샷. 재입장한 클라이언트가 화면을 복원하는 데 쓴다.
 *
 * @param content    SONG/CS 용. ROUND_START 이벤트의 content 와 같은 문자열. 라운드 시작 전이면 null
 * @param statusData HANGMAN 용. HANGMAN_ACTION 이벤트의 status 와 같은 배열
 */
public record GameStateDto(
        String gameType,
        String category,
        int totalCount,
        int currentRound,
        int totalRound,
        String content,
        List<String> statusData
) {
}
