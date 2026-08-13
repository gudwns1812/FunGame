package com.fungame.songquiz.domain.event;

import com.fungame.songquiz.domain.ActionResult;
import com.fungame.songquiz.domain.dto.GameContentDto;

/**
 * 행맨 게임 중 플레이어의 액션 결과를 담는 이벤트 객체입니다.
 */
public record HangmanActionEvent(
        Long roomId,
        Long memberId,
        String nickname,
        char letter,
        ActionResult result,
        GameContentDto status
) {
}
