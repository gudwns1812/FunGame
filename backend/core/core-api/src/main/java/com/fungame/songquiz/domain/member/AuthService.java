package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public boolean checkIdDuplicate(String loginId) {
        return memberReader.existsByLoginId(loginId);
    }

    public boolean checkNicknameDuplicate(String nickname) {
        return memberReader.existsByNickname(nickname);
    }

    public Long signup(String loginId, String password, String nickname, String email) {
        if (!PasswordPolicy.isSatisfiedBy(password)) {
            throw new CoreException(ErrorType.PASSWORD_POLICY_VIOLATION, PasswordPolicy.violationMessage());
        }
        if (memberReader.existsByLoginId(loginId)) {
            throw new CoreException(ErrorType.LOGIN_ID_DUPLICATED);
        }
        if (memberReader.existsByNickname(nickname)) {
            throw new CoreException(ErrorType.NICKNAME_DUPLICATED);
        }
        if (memberReader.existsByEmail(email)) {
            throw new CoreException(ErrorType.EMAIL_DUPLICATED);
        }

        Member member = Member.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .email(email)
                .role(Role.USER) // 기본 역할은 USER
                .build();

        return memberWriter.append(member);
    }

    public void login(String loginId, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginId, password);

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void updateNickname(String loginId, String newNickname) {
        if (memberReader.existsByNickname(newNickname)) {
            throw new CoreException(ErrorType.NICKNAME_DUPLICATED);
        }

        Member member = memberReader.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        member.changeNickname(newNickname);
        memberWriter.update(member);

        refreshAuthenticationOf(member);
    }

    public MemberInfo getMyInfo(String loginId) {
        Member member = memberReader.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return member.getInfo();
    }

    private static void refreshAuthenticationOf(Member member) {
        MemberAdapter adapter = new MemberAdapter(member);
        Authentication refreshed = new UsernamePasswordAuthenticationToken(
                adapter,
                member.getPassword(),
                adapter.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(refreshed);
    }
}
