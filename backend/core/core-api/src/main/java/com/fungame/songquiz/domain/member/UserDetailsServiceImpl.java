package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MemberReader memberReader;

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        return memberReader.findByLoginId(loginId)
                .map(MemberAdapter::new)
                .orElseThrow(() -> new CoreException(ErrorType.PLAYER_NOT_FOUND));
    }
}
