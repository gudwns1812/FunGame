package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.enums.PlayerStatus;

public record OnlineMemberInfo(
        Long memberId,
        String nickname,
        PlayerStatus status,
        Long currentRoomId
) {

    public static OnlineMemberInfo of(Member member, MemberLocation location) {
        return new OnlineMemberInfo(
                member.getId(),
                member.getNickname(),
                location.status(),
                location.roomId()
        );
    }
}
