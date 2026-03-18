package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional(readOnly = true)
    public boolean checkIdDuplicate(String loginId) {
        return memberRepository.existsByLoginId(loginId);
    }

    @Transactional
    public Long signup(String loginId, String password, String nickname) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw new CoreException(ErrorType.LOGIN_ID_DUPLICATED);
        }
        if (memberRepository.existsByNickname(nickname)) {
            throw new CoreException(ErrorType.NICKNAME_DUPLICATED);
        }

        Member member = Member.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .role(Role.USER) // 기본 역할은 USER
                .build();

        return memberRepository.save(member).getId();
    }

    @Transactional
    public void login(String loginId, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginId, password);

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Transactional
    public void updateNickname(String loginId, String newNickname) {
        if (memberRepository.existsByNickname(newNickname)) {
            throw new CoreException(ErrorType.NICKNAME_DUPLICATED);
        }

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        member.changeNickname(newNickname);
    }

    @Transactional(readOnly = true)
    public Member getMyInfo(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));
    }
}
