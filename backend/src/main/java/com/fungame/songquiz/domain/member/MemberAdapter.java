package com.fungame.songquiz.domain.member;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

@Getter
public class MemberAdapter extends User {

    private final String loginId;
    private final String nickName;
    private final PlayerStatus status;

    public MemberAdapter(Member member) {
        super(member.getLoginId(), member.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().getKey())));

        loginId = member.getLoginId();
        nickName = member.getNickname();
        status = PlayerStatus.NONE;
    }
}
