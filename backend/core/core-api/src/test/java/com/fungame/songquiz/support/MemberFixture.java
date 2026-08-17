package com.fungame.songquiz.support;

import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.storage.MemberEntity;

public final class MemberFixture {

    private static final String PASSWORD = "password";

    private MemberFixture() {
    }

    public static Member withId(Long id, String nickname) {
        return Member.restore(
                id,
                "login" + id,
                PASSWORD,
                nickname,
                nickname + "@fun-game.club",
                Role.USER);
    }

    public static MemberEntity entityOf(String loginId, String nickname, String email, String password, Role role) {
        return MemberEntity.builder()
                .loginId(loginId)
                .password(password)
                .nickname(nickname)
                .email(email)
                .role(role)
                .build();
    }

    public static MemberEntity entityOf(String name) {
        return entityOf(name, name, name + "@fun-game.club", PASSWORD, Role.USER);
    }
}
