package com.fungame.songquiz.domain.member;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.Assert;

import java.util.Collections;

@Getter
public class MemberAdapter extends User {

    private final Long id;
    private final String loginId;
    private final String nickName;

    public static String principalNameOf(Member member) {
        Assert.notNull(member.getId(), "저장되지 않은 회원은 인증 주체가 될 수 없습니다.");
        return String.valueOf(member.getId());
    }

    public MemberAdapter(Member member) {
        super(principalNameOf(member), member.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().getKey())));

        id = member.getId();
        loginId = member.getLoginId();
        nickName = member.getNickname();
    }
}
