package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.room.MemberLocation;
import com.fungame.songquiz.enums.PlayerStatus;

public record OnlineMemberInfo(
        Long memberId,
        String nickname,
        PlayerStatus status,
        Long currentRoomId
) {

    public static OnlineMemberInfo of(MemberProfile profile, MemberLocation location) {
        return new OnlineMemberInfo(
                profile.memberId(),
                profile.nickname(),
                location.status(),
                location.roomId()
        );
    }
}
