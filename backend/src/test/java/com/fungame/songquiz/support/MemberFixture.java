package com.fungame.songquiz.support;

import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.Role;
import org.springframework.test.util.ReflectionTestUtils;

public final class MemberFixture {

    private MemberFixture() {
    }

    public static Member withId(Long id, String nickname) {
        Member member = Member.builder()
                .loginId("login" + id)
                .password("password")
                .nickname(nickname)
                .email(nickname + "@fun-game.club")
                .role(Role.USER)
                .build();

        ReflectionTestUtils.setField(member, "id", id);

        return member;
    }
}
