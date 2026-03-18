package com.fungame.songquiz.domain.dto;

import com.fungame.songquiz.domain.member.Member;
import com.fungame.songquiz.domain.member.Role;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberInfo {
    private String loginId;
    private String nickname;
    private Role role;

    @Builder
    private MemberInfo(String loginId, String nickname, Role role) {
        this.loginId = loginId;
        this.nickname = nickname;
        this.role = role;
    }

    public static MemberInfo from(Member member) {
        return MemberInfo.builder()
                .loginId(member.getLoginId())
                .nickname(member.getNickname())
                .role(member.getRole())
                .build();
    }
}
