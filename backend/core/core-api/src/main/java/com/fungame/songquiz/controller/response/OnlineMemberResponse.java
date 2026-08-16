package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.member.OnlineMemberInfo;
import com.fungame.songquiz.enums.PlayerStatus;

import java.util.List;

public record OnlineMemberResponse(
        Long memberId,
        String nickname,
        PlayerStatus status,
        Long currentRoomId
) {

    public static OnlineMemberResponse from(OnlineMemberInfo info) {
        return new OnlineMemberResponse(info.memberId(), info.nickname(), info.status(), info.currentRoomId());
    }

    public static List<OnlineMemberResponse> listFrom(List<OnlineMemberInfo> members) {
        return members.stream()
                .map(OnlineMemberResponse::from)
                .toList();
    }
}
