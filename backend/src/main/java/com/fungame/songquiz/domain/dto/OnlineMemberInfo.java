package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.PlayerStatus;

public record OnlineMemberInfo(
        Long memberId,
        String nickname,
        PlayerStatus status,
        Long currentRoomId
) {

    public static OnlineMemberInfo from(Member member) {
        return new OnlineMemberInfo(
                member.getId(),
                member.getNickname(),
                member.getStatus(),
                member.getCurrentRoomId()
        );
    }
}
