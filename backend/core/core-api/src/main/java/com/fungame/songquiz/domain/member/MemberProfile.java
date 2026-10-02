package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;

import java.io.Serializable;

public record MemberProfile(
        Long memberId,
        String nickname,
        Role role
) implements Serializable {

    public static MemberProfile from(Member member) {
        return member.getInfo().profile();
    }
}
