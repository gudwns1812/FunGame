package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.member.MemberInfo;
import com.fungame.songquiz.enums.Role;

public record MemberResponse(
        Long id,
        String loginId,
        String nickname,
        String email,
        Role role
) {

    public static MemberResponse from(MemberInfo info) {
        return new MemberResponse(info.id(), info.loginId(), info.nickname(), info.email(), info.role());
    }
}
