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
    private Long id;
    private String loginId;
    private String nickname;
    private String email;
    private Role role;

    @Builder
    private MemberInfo(Long id, String loginId, String nickname, String email, Role role) {
        this.id = id;
        this.loginId = loginId;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
    }

    public static MemberInfo from(Member member) {
        return MemberInfo.builder()
                .id(member.getId())
                .loginId(member.getLoginId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .role(member.getRole())
                .build();
    }
}
