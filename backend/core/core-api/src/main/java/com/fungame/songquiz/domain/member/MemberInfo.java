package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import lombok.Builder;

@Builder
public record MemberInfo(
        Long id,
        String loginId,
        String nickname,
        String email,
        Role role
) {

    public MemberInfo withNickname(String newNickname) {
        return new MemberInfo(id, loginId, newNickname, email, role);
    }

    public MemberInfo withRole(Role newRole) {
        return new MemberInfo(id, loginId, nickname, email, newRole);
    }
}
