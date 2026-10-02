package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;

public record MemberInfo(
        MemberProfile profile,
        String loginId,
        String email
) {

    public static MemberInfo of(Long id, String loginId, String nickname, String email, Role role) {
        return new MemberInfo(new MemberProfile(id, nickname, role), loginId, email);
    }

    public Long id() {
        return profile.memberId();
    }

    public String nickname() {
        return profile.nickname();
    }

    public Role role() {
        return profile.role();
    }

    public MemberInfo withNickname(String newNickname) {
        return new MemberInfo(new MemberProfile(id(), newNickname, role()), loginId, email);
    }

    public MemberInfo withRole(Role newRole) {
        return new MemberInfo(new MemberProfile(id(), nickname(), newRole), loginId, email);
    }
}
